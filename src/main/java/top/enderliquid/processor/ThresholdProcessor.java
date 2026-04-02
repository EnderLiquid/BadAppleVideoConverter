package top.enderliquid.processor;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import top.enderliquid.BadAppleVideoConverter;
import top.enderliquid.utils.ImageUtils;

public class ThresholdProcessor implements FrameProcessor {
    private int targetWidth;
    private int targetHeight;
    private double thresholdValue;
    
    private Mat resizedFrame;
    private Mat grayFrame;
    private Mat binaryFrame;
    private byte[] pixels;
    
    private boolean claheEnabled;
    private CLAHE clahe;
    private boolean usmEnabled;
    private double usmRadius;
    private double usmAmount;
    
    @Override
    public void init(int targetWidth, int targetHeight, BadAppleVideoConverter.ConvertConfig config) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.thresholdValue = config.thresholdValue();
        
        this.claheEnabled = config.claheEnabled();
        this.usmEnabled = config.usmEnabled();
        this.usmRadius = config.usmRadius();
        this.usmAmount = config.usmAmount();
        
        this.resizedFrame = new Mat();
        this.grayFrame = new Mat();
        this.binaryFrame = new Mat();
        this.pixels = new byte[targetWidth * targetHeight];
        
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
        
        Imgproc.threshold(grayFrame, binaryFrame, thresholdValue, 255, Imgproc.THRESH_BINARY);
        
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