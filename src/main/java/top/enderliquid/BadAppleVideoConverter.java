package top.enderliquid;

import nu.pattern.OpenCV;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BadAppleVideoConverter {

    public static final int MAX_WIDTH = 8192;
    public static final int MAX_HEIGHT = 8192;
    public static final int MAX_FPS = 240;
    public static final String CONFIG_PATH = "./config.properties";
    public static final String DEFAULT_INPUT_PATH = "./input/bad_apple.mp4";
    public static final String DEFAULT_OUTPUT_PATH = "./output/bad_apple.bin";
    public static final int DEFAULT_TARGET_WIDTH = 80;
    public static final int DEFAULT_TARGET_HEIGHT = 60;
    public static final double DEFAULT_TARGET_FPS = 10.00;

    static {
        OpenCV.loadLocally();
    }

    @Config.Sources({"file:" + CONFIG_PATH})
    public interface ConvertConfig extends Config {
        String inputPath();
        String outputPath();
        Integer targetWidth();
        Integer targetHeight();
        Double targetFps();
    }

    private static ConvertConfig initConfig() {
        try {
            Path configFilePath = Paths.get(CONFIG_PATH);
            if (!Files.exists(configFilePath)) {
                System.err.println("错误: 配置文件不存在");
                // 确保目录存在
                Files.createDirectories(configFilePath.getParent());
                // 创建默认配置文件
                FileWriter writer = new FileWriter(configFilePath.toFile());
                try (writer) {
                    writer.write(String.format("inputPath = %s%n", DEFAULT_INPUT_PATH));
                    writer.write(String.format("outputPath = %s%n", DEFAULT_OUTPUT_PATH));
                    writer.write(String.format("targetWidth = %d%n", DEFAULT_TARGET_WIDTH));
                    writer.write(String.format("targetHeight = %d%n", DEFAULT_TARGET_HEIGHT));
                    writer.write(String.format("targetFps = %.2f%n", DEFAULT_TARGET_FPS));
                }
                System.out.println("已创建默认配置文件: " + CONFIG_PATH);
                System.out.println("请编辑配置文件后重新运行");
                System.exit(0);
            }
        } catch (IOException e) {
            System.err.println("错误: 初始化配置文件失败: " + e.getMessage());
            System.exit(1);
        }
        // 读取配置
        ConvertConfig config = ConfigFactory.create(ConvertConfig.class);
        // 校验配置参数
        List<String> invalidFields = new ArrayList<>();
        if (config.inputPath() == null) invalidFields.add("inputPath");
        if (config.outputPath() == null) invalidFields.add("outputPath");
        if (config.targetWidth() == null || config.targetWidth() < 1) invalidFields.add("targetWidth");
        if (config.targetHeight() == null || config.targetHeight() < 1) invalidFields.add("targetHeight");
        if (config.targetFps() == null || config.targetFps() <= 0) invalidFields.add("targetFps");
        if (!invalidFields.isEmpty()) {
            throw new RuntimeException(
                    String.format("配置参数错误: %s", String.join(", ", invalidFields))
            );
        }
        return config;
    }

    public static void main(String[] args) {
        try {
            ConvertConfig config = initConfig();
            convertVideoToFile(config.inputPath(), config.outputPath(), config.targetWidth(), config.targetHeight(), config.targetFps());
        } catch (RuntimeException e) {
            System.err.printf("错误: %s%n", e.getMessage());
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
     * @param inputPath    视频路径
     * @param targetWidth  目标宽度
     * @param targetHeight 目标高度
     * @param targetFps    目标帧率
     * @param outputPath   输出的 .bin 文件路径
     */
    public static void convertVideoToFile(String inputPath, String outputPath, int targetWidth, int targetHeight, double targetFps) {
        Path inputFilePath, outputFilePath;
        try {
            inputFilePath = Paths.get(inputPath);
            outputFilePath = Paths.get(outputPath);
        } catch (InvalidPathException e) {
            throw new RuntimeException("文件路径无效", e);
        }
        System.out.printf("源路径: %s%n", inputPath);
        System.out.printf("目标路径: %s%n", outputPath);
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
        Mat frame = new Mat();
        Mat resizedFrame = new Mat();
        Mat grayFrame = new Mat();
        Mat binaryFrame = new Mat();
        // 用于读取像素数据的临时数组
        byte[] pixels = new byte[targetWidth * targetHeight];
        // 计算每一帧在二进制文件中占用的字节数 (向上取整)
        int bytesPerFrame = (targetWidth * targetHeight + 7) / 8;
        // 实际写入文件的位打包数组
        byte[] packedBytes = new byte[bytesPerFrame];
        System.out.println("开始处理...");
        System.out.printf("源: %d * %d @ %.2f fps, 总帧数: %d%n", srcWidth, srcHeight, srcFps, srcTotalFrames);
        System.out.printf("目标: %d * %d @ %.2f fps, 总帧数: %d%n", targetWidth, targetHeight, targetFps, targetTotalFrames);
        System.out.printf("每帧数据大小: %d bytes%n", bytesPerFrame);
        try {
            Files.createDirectories(outputFilePath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("创建目标路径失败", e);
        }
        // 使用 BufferedOutputStream 写入二进制文件
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
                    // 1. 缩放
                    Imgproc.resize(frame, resizedFrame, new Size(targetWidth, targetHeight));
                    // 2. 转换为灰度图
                    Imgproc.cvtColor(resizedFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                    // 3. 二值化 (阈值128, 0黑 255白)
                    Imgproc.threshold(grayFrame, binaryFrame, 128, 255, Imgproc.THRESH_BINARY);
                    // 4. 获取像素数据 (覆盖临时数组原有内容)
                    binaryFrame.get(0, 0, pixels);
                    // 5. 位打包
                    for (int p = 0; p < pixels.length; p++) {
                        // 将有符号byte (-128 - 127) 转换为无符号int (0 - 255)
                        int val = pixels[p] & 0xFF;
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
                // 6. 将位打包数组写入文件
                bos.write(packedBytes);
                // 7. 控制台帧预览与进度播报
                if (i == 1 || i % 100 == 0 || i == targetTotalFrames) {
                    printFrameFromBytes(packedBytes, targetWidth, targetHeight);
                    System.out.printf("已处理: %d / %d 帧%n", i, targetTotalFrames);
                }
            }
            System.out.println("文件写入完成: " + outputFilePath);
        } catch (IOException e) {
            throw new RuntimeException("文件读写发生错误", e);
        } finally {
            frame.release();
            resizedFrame.release();
            grayFrame.release();
            binaryFrame.release();
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
}