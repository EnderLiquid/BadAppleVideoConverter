package top.enderliquid.processor;

import org.opencv.core.Mat;
import top.enderliquid.BadAppleVideoConverter;

/**
 * 帧处理器接口
 * 所有返回值 byte[] 必须且只能包含 0 (黑) 或 255 (白) 两种值，长度必须等于 targetWidth * targetHeight
 * 关键约束:
 * - srcFrame 由调用方管理，Processor 不得修改或释放 srcFrame，仅读取其数据
 * - 内部 Mat 对象在 process() 结束后不应被外部引用，生命周期由 Processor 自行管理
 */
public interface FrameProcessor {
    /**
     * 初始化处理器，分配可复用的 Mat 内存
     *
     * @param targetWidth  目标宽度
     * @param targetHeight 目标高度
     * @param config       配置对象
     */
    void init(int targetWidth, int targetHeight, BadAppleVideoConverter.ConvertConfig config);

    /**
     * 处理单帧画面
     *
     * @param srcFrame 原始视频帧 (BGR 格式，原始分辨率)，由调用方管理，不得修改或释放
     * @return 仅包含 0 或 255 的一维像素数组，长度 = targetWidth * targetHeight
     */
    byte[] process(Mat srcFrame);

    /**
     * 释放该处理器内部持有的所有 OpenCV 资源
     * 必须在所有 process() 调用结束后调用
     */
    void release();
}