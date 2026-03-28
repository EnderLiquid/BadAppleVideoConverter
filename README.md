# BadAppleVideoConverter

将视频转换为二进制帧序列的工具，可用于 OLED 播放。

## 功能

- 视频缩放、灰度化、二值化处理
- 可配置目标分辨率、帧率与二值化阈值
- 位打包压缩输出，节省存储空间

## 配置格式

配置文件 `config.properties` 支持以下参数：

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

# 二值化阈值
# 范围: 0-255
# 像素灰度值 > 此阈值判定为白色（1），≤ 此阈值判定为黑色（0）
# 值越小，越多的像素被判定为白色；值越大，越多的像素被判定为黑色
thresholdValue=127.00
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