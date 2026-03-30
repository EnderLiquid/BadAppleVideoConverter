package top.enderliquid.processor;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BlueNoiseDitherProcessor extends AbstractOrderedDitherProcessor {
    
    @Override
    protected Mat createBasePattern() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("HDR_LA_0.png");
            if (is == null) {
                throw new RuntimeException("无法加载蓝噪声资源文件: HDR_LA_0.png");
            }
            
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            
            byte[] imageData = os.toByteArray();
            Mat imageMat = new Mat(imageData.length, 1, CvType.CV_8UC1);
            imageMat.put(0, 0, imageData);
            
            Mat blueNoise = Imgcodecs.imdecode(imageMat, Imgcodecs.IMREAD_GRAYSCALE);
            imageMat.release();
            
            if (blueNoise.empty()) {
                throw new RuntimeException("蓝噪声图像解码失败");
            }
            
            return blueNoise;
        } catch (IOException e) {
            throw new RuntimeException("读取蓝噪声资源文件失败: " + e.getMessage(), e);
        }
    }
}