package top.enderliquid.enums;

public enum ConvertMode {
    THRESHOLD,          // 全局固定阈值模式
    DITHER_BAYER,       // Bayer 有序抖动模式
    DITHER_BLUE_NOISE,  // 蓝噪声抖动模式
    DITHER_FS,          // Floyd-Steinberg 误差扩散模式
    CANNY_EDGE          // Canny 边缘提取模式
}