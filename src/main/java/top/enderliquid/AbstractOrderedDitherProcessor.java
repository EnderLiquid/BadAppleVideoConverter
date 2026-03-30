package top.enderliquid;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

public abstract class AbstractOrderedDitherProcessor implements FrameProcessor {
    protected int targetWidth;
    protected int targetHeight;
    
    protected Mat tiledThresholdMap;
    protected Mat resizedFrame;
    protected Mat grayFrame;
    protected Mat binaryResult;
    protected byte[] pixels;
    
    protected boolean claheEnabled;
    protected CLAHE clahe;
    protected boolean usmEnabled;
    protected double usmRadius;
    protected double usmAmount;
    
    protected abstract Mat createBasePattern();
    
    @Override
    public void init(int targetWidth, int targetHeight, BadAppleVideoConverter.ConvertConfig config) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        
        this.claheEnabled = config.claheEnabled();
        this.usmEnabled = config.usmEnabled();
        this.usmRadius = config.usmRadius();
        this.usmAmount = config.usmAmount();
        
        Mat basePattern = createBasePattern();
        
        int repeatX = (int) Math.ceil((double) targetWidth / basePattern.cols());
        int repeatY = (int) Math.ceil((double) targetHeight / basePattern.rows());
        
        Mat tiledPattern = new Mat();
        Core.repeat(basePattern, repeatY, repeatX, tiledPattern);
        
        this.tiledThresholdMap = tiledPattern.submat(0, targetHeight, 0, targetWidth).clone();
        
        this.resizedFrame = new Mat();
        this.grayFrame = new Mat();
        this.binaryResult = new Mat();
        this.pixels = new byte[targetWidth * targetHeight];
        
        if (claheEnabled) {
            this.clahe = Imgproc.createCLAHE(config.claheClipLimit(), 
                new Size(config.claheGridSize(), config.claheGridSize()));
        }
        
        basePattern.release();
        tiledPattern.release();
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
        
        Core.compare(grayFrame, tiledThresholdMap, binaryResult, Core.CMP_GT);
        
        binaryResult.get(0, 0, pixels);
        
        return pixels;
    }
    
    @Override
    public void release() {
        if (tiledThresholdMap != null) tiledThresholdMap.release();
        if (resizedFrame != null) resizedFrame.release();
        if (grayFrame != null) grayFrame.release();
        if (binaryResult != null) binaryResult.release();
    }
}