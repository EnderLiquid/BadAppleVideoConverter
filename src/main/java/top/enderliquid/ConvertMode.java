package top.enderliquid;

public enum ConvertMode {
    THRESHOLD,      // 全局固定阈值模式 (默认)
    DITHER_BAYER,   // Bayer 有序抖动模式
    CANNY_EDGE      // Canny 边缘提取模式
}