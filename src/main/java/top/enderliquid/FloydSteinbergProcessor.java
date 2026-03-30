package top.enderliquid;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

public class FloydSteinbergProcessor implements FrameProcessor {
    private int targetWidth;
    private int targetHeight;
    
    private Mat resizedFrame;
    private Mat grayFrame;
    private byte[] rawPixels;
    private int[] grayPixels;
    private byte[] outputPixels;
    
    private boolean claheEnabled;
    private CLAHE clahe;
    private boolean usmEnabled;
    private double usmRadius;
    private double usmAmount;
    
    @Override
    public void init(int targetWidth, int targetHeight, BadAppleVideoConverter.ConvertConfig config) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        
        this.claheEnabled = config.claheEnabled();
        this.usmEnabled = config.usmEnabled();
        this.usmRadius = config.usmRadius();
        this.usmAmount = config.usmAmount();
        
        this.resizedFrame = new Mat();
        this.grayFrame = new Mat();
        
        int pixelCount = targetWidth * targetHeight;
        this.rawPixels = new byte[pixelCount];
        this.grayPixels = new int[pixelCount];
        this.outputPixels = new byte[pixelCount];
        
        if (claheEnabled) {
            this.clahe = Imgproc.createCLAHE(config.claheClipLimit(),
                new Size(config.claheGridSize(), config.claheGridSize()));
        }
    }
    
    @Override
    public byte[] process(Mat srcFrame) {
        Imgproc.resize(srcFrame, resizedFrame, new Size(targetWidth, targetHeight));
        Imgproc.cvtColor(resizedFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        
        if (claheEnabled) {
            clahe.apply(grayFrame, grayFrame);
        }
        
        if (usmEnabled) {
            ImageUtils.applyUSM(grayFrame, usmRadius, usmAmount);
        }
        
        grayFrame.get(0, 0, rawPixels);
        
        for (int i = 0; i < rawPixels.length; i++) {
            grayPixels[i] = rawPixels[i] & 0xFF;
        }
        
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int idx = y * targetWidth + x;
                int oldPixel = grayPixels[idx];
                int newPixel = oldPixel > 127 ? 255 : 0;
                outputPixels[idx] = (byte) newPixel;
                int error = oldPixel - newPixel;
                
                if (x + 1 < targetWidth) {
                    grayPixels[idx + 1] = clamp(grayPixels[idx + 1] + (error * 7) / 16);
                }
                if (y + 1 < targetHeight && x - 1 >= 0) {
                    grayPixels[idx + targetWidth - 1] = clamp(grayPixels[idx + targetWidth - 1] + (error * 3) / 16);
                }
                if (y + 1 < targetHeight) {
                    grayPixels[idx + targetWidth] = clamp(grayPixels[idx + targetWidth] + (error * 5) / 16);
                }
                if (y + 1 < targetHeight && x + 1 < targetWidth) {
                    grayPixels[idx + targetWidth + 1] = clamp(grayPixels[idx + targetWidth + 1] + error / 16);
                }
            }
        }
        
        return outputPixels;
    }
    
    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
    
    @Override
    public void release() {
        if (resizedFrame != null) resizedFrame.release();
        if (grayFrame != null) grayFrame.release();
    }
}