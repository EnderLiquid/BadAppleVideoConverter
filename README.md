# BadAppleVideoConverter

将视频转换为二进制帧序列的工具，可用于 OLED 播放。

## 功能

- 多种二值化处理模式：阈值二值化、Bayer抖动、蓝噪声抖动、Floyd-Steinberg误差扩散、Canny边缘检测
- 可选预处理：CLAHE对比度增强、USM锐化
- 可配置目标分辨率、帧率与处理参数
- 位打包压缩输出，节省存储空间

## 处理模式

| 模式                  | 说明                          |
| ------------------- | --------------------------- |
| `THRESHOLD`         | 全局固定阈值二值化，最简单直接             |
| `DITHER_BAYER`      | Bayer 4x4 有序抖动，产生伪灰度网格效果    |
| `DITHER_BLUE_NOISE` | 蓝噪声抖动，更自然的伪灰度效果             |
| `DITHER_FS`         | Floyd-Steinberg 误差扩散，高质量伪灰度 |
| `CANNY_EDGE`        | Canny 边缘检测，提取轮廓线条           |

## 配置格式

配置文件 `config.properties` 支持以下参数：

### 基础配置

```properties
# 输入视频文件路径
# 支持 OpenCV 可解析的视频格式（如 MP4、AVI 等）
inputPath=./input/bad_apple.mp4

# 输出二进制文件路径
# 目录不存在时会自动创建
outputPath=./output/bad_apple_video.bin

# 目标宽度（像素）
# 范围: ≥1，上限 8192
# 若大于源视频宽度，将自动使用源宽度
targetWidth=88

# 目标高度（像素）
# 范围: ≥1，上限 8192
# 若大于源视频高度，将自动使用源高度
targetHeight=64

# 目标帧率
# 范围: >0，上限 240
# 若大于源视频帧率，将自动使用源帧率
targetFps=30.00

# 处理模式
# 可选值: THRESHOLD, DITHER_BAYER, DITHER_BLUE_NOISE, DITHER_FS, CANNY_EDGE
mode=THRESHOLD
```

### 阈值二值化模式配置

```properties
# 二值化阈值
# 范围: 0-255
# 像素灰度值 > 此阈值判定为白色（1），≤ 此阈值判定为黑色（0）
thresholdValue=127.00
```

### CLAHE 预处理配置

CLAHE（对比度受限自适应直方图均衡化）可增强图像对比度，对抖动模式效果显著。

```properties
# 是否启用 CLAHE 预处理
# Canny 模式强制禁用此选项
claheEnabled=true

# 对比度限制
# 范围: ≥0，0 或负值表示无限制
# 值越大，对比度增强越明显，但可能引入噪声
claheClipLimit=1.5

# 网格大小
# 范围: ≥1
# 图像被分割为 gridSize × gridSize 个区块进行处理
claheGridSize=8
```

### USM 锐化预处理配置

USM（反锐化掩膜）可增强图像边缘锐度。

```properties
# 是否启用 USM 锐化预处理
# Canny 模式强制禁用此选项
usmEnabled=true

# 模糊半径
# 范围: >0
# 值越大，锐化范围越宽
usmRadius=1.0

# 锐化强度
# 范围: >0
# 值越大，锐化效果越强
usmAmount=1.2
```

### Canny 边缘检测模式配置

```properties
# 是否先缩放后检测
# true: 先缩放到目标分辨率再进行边缘检测（性能高）
# false: 先在全分辨率下检测边缘再缩放（保留更多细节，但缩放后需二次二值化）
cannyPreScale=true

# 是否膨胀线条
# 膨胀可使边缘线条变粗，适合低分辨率显示
cannyDilate=false

# 膨胀核大小
# 范围: ≥2
cannyDilateSize=2

# Canny 低阈值
# 范围: 0-255
# 低于此值的边缘被丢弃
cannyThreshold1=50.00

# Canny 高阈值
# 范围: 0-255
# 高于此值的边缘被保留，介于两阈值之间的边缘仅在连接到强边缘时保留
cannyThreshold2=150.00

# Sobel 算子孔径大小
# 可选值: 3, 5, 7
# 值越大，边缘检测越平滑但可能丢失细节
cannyApertureSize=3

# 是否启用高斯预模糊
# 在灰度转换后、边缘检测前应用，可减少噪声干扰
cannyBlurEnabled=true

# 高斯模糊核大小
# 范围: 正奇数 (3, 5, 7...)
# 值越大，模糊程度越高，边缘越平滑
cannyBlurSize=3

# 是否使用 L2 范数计算梯度
# true: 使用更精确的 L2 范数 (√(Gx²+Gy²))，边缘检测更精确但计算稍慢（默认）
# false: 使用近似的 L1 范数 (|Gx|+|Gy|)，计算更快
cannyL2Gradient=true
```

## 输出格式

二进制文件由文件头和帧数据组成：

**文件头** (16 字节, 小端):

| 偏移    | 类型    | 说明  |
| ----- | ----- | --- |
| 0-3   | int   | 宽度  |
| 4-7   | int   | 高度  |
| 8-11  | float | 帧率  |
| 12-15 | int   | 总帧数 |

**帧数据**: 每帧 `(宽×高+7)/8` 字节，位序 LSB First，1=白，0=黑。

## 使用方法

1. 运行程序，自动生成 `config.properties`
2. 编辑配置文件
3. 再次运行执行转换

## 依赖

- Java 21
- [OpenCV](https://opencv.org/) - 视频处理
- [owner](https://github.com/lviggiano/owner) - 配置管理

## 许可证

MIT License