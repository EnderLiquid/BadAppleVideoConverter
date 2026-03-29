package top.enderliquid;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Canny 边缘提取处理器
 * 支持先缩放/后缩放和线条膨胀选项
 */
public class CannyProcessor implements FrameProcessor {
    private Mat grayFrame;           // CV_8UC1: 灰度图
    private Mat edgesFrame;          // CV_8UC1: Canny 边缘结果
    private Mat resizedFrameBGR;     // CV_8UC3: 分支A专用，缩放后的BGR帧
    private Mat resizedFrameGray;    // CV_8UC1: 分支B专用，缩放后的灰度帧
    private Mat dilateKernel;        // CV_8UC1: 膨胀核 (可选)
    private byte[] pixels;           // 输出像素数组
    private int targetWidth;
    private int targetHeight;
    private boolean preScale;        // true: 先缩放后Canny; false: 先Canny后缩放
    private boolean dilate;          // 是否膨胀线条
    private double threshold1;       // Canny 低阈值
    private double threshold2;       // Canny 高阈值
    private int aperture;         // Sobel 算子的孔径大小

    @Override
    public void init(int targetWidth, int targetHeight, BadAppleVideoConverter.ConvertConfig config) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.preScale = config.cannyPreScale();
        this.dilate = config.cannyDilate();
        this.threshold1 = config.cannyThreshold1();
        this.threshold2 = config.cannyThreshold2();
        this.aperture = config.cannyApertureSize();

        // 膨胀核大小
        int dilateSize = config.cannyDilateSize();

        // 创建 Mat 对象
        grayFrame = new Mat();
        edgesFrame = new Mat();
        resizedFrameBGR = new Mat();
        resizedFrameGray = new Mat();

        // 若开启膨胀，创建矩形核
        if (dilate) {
            dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(dilateSize, dilateSize));
        }

        // 初始化输出像素数组
        pixels = new byte[targetWidth * targetHeight];
    }

    @Override
    public byte[] process(Mat srcFrame) {
        if (preScale) {
            // 分支 A: 先缩放后 Canny
            // resize(srcFrame, resizedFrameBGR) -> cvtColor -> Canny -> dilate? -> get
            Imgproc.resize(srcFrame, resizedFrameBGR, new Size(targetWidth, targetHeight));
            Imgproc.cvtColor(resizedFrameBGR, grayFrame, Imgproc.COLOR_BGR2GRAY);
            Imgproc.Canny(grayFrame, edgesFrame, threshold1, threshold2, aperture);

            if (dilate) {
                Imgproc.dilate(edgesFrame, edgesFrame, dilateKernel);
            }

            edgesFrame.get(0, 0, pixels);
        } else {
            // 分支 B: 先 Canny 后缩放
            // cvtColor(srcFrame) -> Canny -> dilate? -> resize -> threshold(127) -> get
            Imgproc.cvtColor(srcFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);
            Imgproc.Canny(grayFrame, edgesFrame, threshold1, threshold2, aperture);

            if (dilate) {
                Imgproc.dilate(edgesFrame, edgesFrame, dilateKernel);
            }

            Imgproc.resize(edgesFrame, resizedFrameGray, new Size(targetWidth, targetHeight));

            // 关键: resize 会产生灰边抗锯齿，必须再次 threshold 强制二值化
            Imgproc.threshold(resizedFrameGray, resizedFrameGray, 127, 255, Imgproc.THRESH_BINARY);

            resizedFrameGray.get(0, 0, pixels);
        }

        return pixels;
    }

    @Override
    public void release() {
        if (grayFrame != null) grayFrame.release();
        if (edgesFrame != null) edgesFrame.release();
        if (resizedFrameBGR != null) resizedFrameBGR.release();
        if (resizedFrameGray != null) resizedFrameGray.release();
        if (dilateKernel != null) dilateKernel.release();
    }
}