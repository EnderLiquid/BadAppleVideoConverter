package top.enderliquid;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * 经典阈值二值化处理器
 * 实现 resize -> grayscale -> threshold 流程
 */
public class ThresholdProcessor implements FrameProcessor {
    private Mat resizedFrame;   // CV_8UC3: 缩放后的BGR帧
    private Mat grayFrame;      // CV_8UC1: 灰度图
    private Mat binaryFrame;    // CV_8UC1: 二值化结果
    private byte[] pixels;      // 输出像素数组
    private int targetWidth;
    private int targetHeight;
    private double thresholdValue;

    @Override
    public void init(int targetWidth, int targetHeight, BadAppleVideoConverter.ConvertConfig config) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.thresholdValue = config.thresholdValue();

        // 创建 Mat 对象
        resizedFrame = new Mat();
        grayFrame = new Mat();
        binaryFrame = new Mat();

        // 初始化输出像素数组
        pixels = new byte[targetWidth * targetHeight];
    }

    @Override
    public byte[] process(Mat srcFrame) {
        // 1. 缩放到目标分辨率
        Imgproc.resize(srcFrame, resizedFrame, new Size(targetWidth, targetHeight));

        // 2. 转换为灰度图
        Imgproc.cvtColor(resizedFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        // 3. 二值化 (阈值 thresholdValue, 输出 0 或 255)
        Imgproc.threshold(grayFrame, binaryFrame, thresholdValue, 255, Imgproc.THRESH_BINARY);

        // 4. 获取像素数据
        binaryFrame.get(0, 0, pixels);

        return pixels;
    }

    @Override
    public void release() {
        if (resizedFrame != null) resizedFrame.release();
        if (grayFrame != null) grayFrame.release();
        if (binaryFrame != null) binaryFrame.release();
    }
}