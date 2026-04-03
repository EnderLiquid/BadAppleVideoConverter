package top.enderliquid.processor;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;
import java.io.InputStream;

public class BlueNoiseDitherProcessor extends AbstractOrderedDitherProcessor {
    private static final String BLUE_NOISE_IMAGE_NAME = "HDR_LA_0.png";

    @Override
    protected Mat createBasePattern() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(BLUE_NOISE_IMAGE_NAME)) {
            if (is == null) {
                throw new RuntimeException("无法加载蓝噪声资源文件" + BLUE_NOISE_IMAGE_NAME);
            }

            // 1. 从输入流读取全部字节
            byte[] imageData = is.readAllBytes();

            // 2. 从字节数组创建 Mat 对象
            MatOfByte mob = new MatOfByte(imageData);
            Mat blueNoise = Imgcodecs.imdecode(mob, Imgcodecs.IMREAD_GRAYSCALE); // 以单通道模式读取灰度图像
            mob.release();
            if (blueNoise.empty()) {
                throw new RuntimeException("蓝噪声图像解码失败");
            }

            return blueNoise;
        } catch (IOException e) {
            throw new RuntimeException("读取蓝噪声资源文件失败", e);
        }
    }
}