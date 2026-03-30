package top.enderliquid.utils;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public final class ImageUtils {
    
    private ImageUtils() {}
    
    public static void applyUSM(Mat src, double radius, double amount) {
        Mat blur = new Mat();
        Imgproc.GaussianBlur(src, blur, new Size(0, 0), radius);
        double alpha = 1.0 + amount;
        double beta = -amount;
        double gamma = 0.0;
        Core.addWeighted(src, alpha, blur, beta, gamma, src);
        blur.release();
    }
}