package top.enderliquid;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class BayerDitherProcessor extends AbstractOrderedDitherProcessor {
    
    @Override
    protected Mat createBasePattern() {
        Mat bayer = new Mat(4, 4, CvType.CV_8UC1);
        byte[] bayerData = {
             (byte)0,   (byte)128, (byte)32,  (byte)160,
             (byte)192, (byte)64,  (byte)224, (byte)96,
             (byte)48,  (byte)176, (byte)16,  (byte)144,
             (byte)240, (byte)112, (byte)208, (byte)80
        };
        bayer.put(0, 0, bayerData);
        return bayer;
    }
}