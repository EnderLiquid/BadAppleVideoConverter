package top.enderliquid;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Bayer 4x4 有序抖动处理器
 * 利用 4x4 矩阵产生伪灰度网格效果
 */
public class BayerProcessor implements FrameProcessor {
    // 4x4 Bayer 矩阵 (已映射到 0-255)
    private static final int[][] BAYER_MATRIX = {
            {15, 135, 45, 165},
            {195, 75, 225, 105},
            {60, 180, 30, 150},
            {240, 120, 210, 90}
    };

    private Mat resizedFrame;   // CV_8UC3: 缩放后的BGR帧
    private Mat grayFrame;      // CV_8UC1: 灰度图
    private byte[] grayPixels;  // 灰度像素临时数组
    private byte[] outputPixels;// 输出像素数组
    private int targetWidth;
    private int targetHeight;

    @Override
    public void init(int targetWidth, int targetHeight, BadAppleVideoConverter.ConvertConfig config) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;

        // 创建 Mat 对象
        resizedFrame = new Mat();
        grayFrame = new Mat();

        // 初始化像素数组
        int pixelCount = targetWidth * targetHeight;
        grayPixels = new byte[pixelCount];
        outputPixels = new byte[pixelCount];
    }

    @Override
    public byte[] process(Mat srcFrame) {
        // 1. 缩放到目标分辨率
        Imgproc.resize(srcFrame, resizedFrame, new Size(targetWidth, targetHeight));

        // 2. 转换为灰度图
        Imgproc.cvtColor(resizedFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        // 3. 获取灰度像素数据
        grayFrame.get(0, 0, grayPixels);

        // 4. 应用 Bayer 抖动算法
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int idx = y * targetWidth + x;
                int grayValue = grayPixels[idx] & 0xFF; // Java byte 转无符号 int
                int threshold = BAYER_MATRIX[y % 4][x % 4];
                outputPixels[idx] = (byte) (grayValue > threshold ? 255 : 0);
            }
        }

        return outputPixels;
    }

    @Override
    public void release() {
        if (resizedFrame != null) resizedFrame.release();
        if (grayFrame != null) grayFrame.release();
    }
}