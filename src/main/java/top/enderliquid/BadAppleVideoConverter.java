package top.enderliquid;

import nu.pattern.OpenCV;
import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BadAppleVideoConverter {

    public static final int MAX_WIDTH = 8192;
    public static final int MAX_HEIGHT = 8192;
    public static final int MAX_FPS = 240;
    public static final String CONFIG_PATH = "./config.properties";

    static {
        OpenCV.loadLocally();
    }

    private static ConvertConfig initConfig() {
        try {
            Path configFilePath = Paths.get(CONFIG_PATH);
            // 创建配置实例（如果文件不存在，此时里面装的都是 @DefaultValue 的值）
            ConvertConfig config = ConfigFactory.create(ConvertConfig.class);
            if (!Files.exists(configFilePath)) {
                // 确保目录存在
                Files.createDirectories(configFilePath.getParent());
                // 使用 Accessible.store() 导出默认值到文件
                try (FileOutputStream out = new FileOutputStream(configFilePath.toFile())) {
                    config.store(out, "BadAppleVideoConverter Config");
                }
                System.out.println("已创建默认配置文件: " + CONFIG_PATH);
                System.out.println("请编辑配置文件后重新运行");
                System.exit(0);
            }
            // 校验配置参数
            List<String> invalidFields = new ArrayList<>();
            if (config.inputPath() == null) invalidFields.add("inputPath");
            if (config.outputPath() == null) invalidFields.add("outputPath");
            if (config.targetWidth() == null || config.targetWidth() < 1) invalidFields.add("targetWidth");
            if (config.targetHeight() == null || config.targetHeight() < 1) invalidFields.add("targetHeight");
            if (config.targetFps() == null || config.targetFps() <= 0) invalidFields.add("targetFps");
            if (config.thresholdValue() == null || config.thresholdValue() < 0 || config.thresholdValue() > 255)
                invalidFields.add("thresholdValue");
            if (!invalidFields.isEmpty()) {
                throw new RuntimeException(
                        String.format("配置参数错误: %s", String.join(", ", invalidFields))
                );
            }
            return config;
        } catch (IOException e) {
            System.err.println("错误: 初始化配置文件失败: " + e.getMessage());
            System.exit(1);
            return null; // 不会执行，但编译器需要
        }
    }

    public static void main(String[] args) {
        try {
            ConvertConfig config = initConfig();
            convertVideoToFile(config);
        } catch (RuntimeException e) {
            System.err.printf("错误: %s%n%s",
                    e.getMessage(),
                    Arrays.stream(e.getStackTrace())
                            .map(StackTraceElement::toString)
                            .collect(Collectors.joining(System.lineSeparator()))
            );
        }
    }

    /**
     * 将视频转换为二进制文件
     * 文件头字段字节序为小端顺序
     * 帧数据位序为LSB First
     * 文件结构:
     * [0-3]   Int   Width
     * [4-7]   Int   Height
     * [8-11]  Float FPS
     * [12-15] Int   FrameCount
     * [16-N]  Bytes Frame Data (每帧固定字节数)
     *
     * @param config 配置对象，包含所有处理参数
     */
    public static void convertVideoToFile(ConvertConfig config) {
        String inputPath = config.inputPath();
        String outputPath = config.outputPath();
        int targetWidth = config.targetWidth();
        int targetHeight = config.targetHeight();
        double targetFps = config.targetFps();
        ConvertMode mode = config.mode();

        Path inputFilePath, outputFilePath;
        try {
            inputFilePath = Paths.get(inputPath);
            outputFilePath = Paths.get(outputPath);
        } catch (InvalidPathException e) {
            throw new RuntimeException("文件路径无效", e);
        }
        System.out.printf("源路径: %s%n", inputPath);
        System.out.printf("目标路径: %s%n", outputPath);
        System.out.printf("处理模式: %s%n", mode);
        if (!Files.exists(inputFilePath)) {
            throw new RuntimeException("源文件不存在");
        }
        VideoCapture capture = new VideoCapture(inputFilePath.toString());
        if (!capture.isOpened()) {
            throw new RuntimeException("无法打开源文件");
        }
        // 获取源视频信息
        double srcFps = capture.get(Videoio.CAP_PROP_FPS);
        int srcTotalFrames = (int) capture.get(Videoio.CAP_PROP_FRAME_COUNT);
        int srcWidth = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int srcHeight = (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        // 校验源视频参数
        if (srcWidth > MAX_WIDTH || srcHeight > MAX_HEIGHT) {
            throw new RuntimeException("源分辨率超出上限");
        }
        if (srcFps > MAX_FPS) {
            throw new RuntimeException("源帧率超出上限");
        }
        // 校验目标参数
        if (targetWidth > srcWidth || targetHeight > srcHeight) {
            System.err.println("警告: 目标分辨率大于源分辨率，将使用源分辨率");
            targetWidth = srcWidth;
            targetHeight = srcHeight;
        }
        if (targetFps > srcFps) {
            System.err.println("警告: 目标帧率大于源帧率，将使用源帧率");
            targetFps = srcFps;
        }
        // 计算总帧数
        double duration = srcTotalFrames / srcFps;
        int targetTotalFrames = (int) (duration * targetFps);

        // 计算每一帧在二进制文件中占用的字节数 (向上取整)
        int bytesPerFrame = (targetWidth * targetHeight + 7) / 8;

        System.out.println("开始处理...");
        System.out.printf("源: %d * %d @ %.2f fps, 总帧数: %d%n", srcWidth, srcHeight, srcFps, srcTotalFrames);
        System.out.printf("目标: %d * %d @ %.2f fps, 总帧数: %d%n", targetWidth, targetHeight, targetFps, targetTotalFrames);
        System.out.printf("每帧数据大小: %d bytes%n", bytesPerFrame);

        try {
            Files.createDirectories(outputFilePath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("创建目标路径失败", e);
        }

        // 策略选择与初始化 (在外层 try-finally 中确保资源释放)
        FrameProcessor processor = null;
        Mat frame = new Mat();
        byte[] packedBytes = new byte[bytesPerFrame];

        try {
            // 根据 mode 选择处理器
            processor = switch (mode) {
                case CANNY_EDGE -> new CannyProcessor();
                case DITHER_BAYER -> new BayerProcessor();
                default -> new ThresholdProcessor();
            };
            processor.init(targetWidth, targetHeight, config);

            // 文件写入
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFilePath.toFile()))) {
                // 写入文件头 (Header)
                ByteBuffer buffer = ByteBuffer.allocate(16);
                buffer.order(ByteOrder.LITTLE_ENDIAN); // 小端模式输出
                buffer.putInt(targetWidth); // 宽 (4 bytes)
                buffer.putInt(targetHeight); // 高 (4 bytes)
                buffer.putFloat((float) targetFps); // FPS (4 bytes, 使用float兼容C语言)
                buffer.putInt(targetTotalFrames); // 总帧数 (4 bytes)
                bos.write(buffer.array());

                for (int i = 1; i <= targetTotalFrames; i++) {
                    // 计算源帧的索引
                    int currentFrameIndex = i - 1;
                    double currentTime = currentFrameIndex / targetFps;
                    int srcFrameIndex = (int) (currentTime * srcFps);
                    capture.set(Videoio.CAP_PROP_POS_FRAMES, srcFrameIndex);

                    // 重置位打包数组
                    Arrays.fill(packedBytes, (byte) 0);

                    if (capture.read(frame) && !frame.empty()) {
                        // 调用策略类处理，获取非0即255的 byte 数组
                        byte[] binaryPixels = processor.process(frame);

                        // 统一的位打包逻辑 (Bit-packing)
                        for (int p = 0; p < binaryPixels.length; p++) {
                            // 将有符号byte (-128 - 127) 转换为无符号int (0 - 255)
                            int val = binaryPixels[p] & 0xFF;
                            // 逻辑: 白色为1, 黑色为0
                            if (val == 255) {
                                // 计算当前像素属于哪个 byte (p / 8) 以及该 byte 中的哪一位 (p % 8)
                                int byteIndex = p / 8;
                                int bitIndex = p % 8;
                                // 使用位运算将位打包数组对应位置 1 (位序为LSB First)
                                packedBytes[byteIndex] |= (byte) (1 << bitIndex);
                            }
                        }
                    } else {
                        System.err.printf("警告: %d / %d 帧为空帧%n", i, targetTotalFrames);
                    }

                    // 将位打包数组写入文件
                    bos.write(packedBytes);

                    // 控制台帧预览与进度播报
                    if (i == 1 || i % 100 == 0 || i == targetTotalFrames) {
                        printFrameFromBytes(packedBytes, targetWidth, targetHeight);
                        System.out.printf("已处理: %d / %d 帧%n", i, targetTotalFrames);
                    }
                }
                System.out.println("文件写入完成: " + outputFilePath);
            } catch (IOException e) {
                throw new RuntimeException("文件读写发生错误", e);
            }
        } finally {
            // 统一释放资源
            if (processor != null) processor.release();
            frame.release();
            capture.release();
        }
    }

    // 辅助方法: 控制台打印帧预览
    private static void printFrameFromBytes(byte[] packedBytes, int width, int height) {
        if (packedBytes == null) return;
        System.out.println("---- 帧预览 ----");
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int byteIndex = index / 8;
                int bitIndex = index % 8;
                // 检查对应字节的对应位是否为 1
                boolean isWhite = (packedBytes[byteIndex] & (1 << bitIndex)) != 0;
                System.out.print(isWhite ? "#" : " ");
            }
            System.out.println();
        }
    }

    @Config.Sources({"file:" + CONFIG_PATH})
    public interface ConvertConfig extends Accessible {
        @DefaultValue("./input/bad_apple.mp4")
        String inputPath();

        @DefaultValue("./output/bad_apple_video.bin")
        String outputPath();

        @DefaultValue("88")
        Integer targetWidth();

        @DefaultValue("64")
        Integer targetHeight();

        @DefaultValue("30.00")
        Double targetFps();

        @DefaultValue("THRESHOLD")
        ConvertMode mode();

        @DefaultValue("127.00")
        Double thresholdValue();

        @DefaultValue("true")
        Boolean cannyPreScale();

        @DefaultValue("false")
        Boolean cannyDilate();

        @DefaultValue("50.0")
        Double cannyThreshold1();

        @DefaultValue("150.0")
        Double cannyThreshold2();
    }
}