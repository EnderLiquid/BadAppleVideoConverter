# BadAppleVideoConverter

将视频转换为二进制帧序列的工具，可用于 OLED 播放。

## 功能

- 视频缩放、灰度化、二值化处理
- 可配置目标分辨率与帧率
- 位打包压缩输出，节省存储空间

## 输出格式

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
2. 编辑配置文件指定参数
3. 再次运行执行转换

```properties
inputPath=./input/bad_apple.mp4
outputPath=./output/bad_apple_video.bin
targetWidth=80
targetHeight=60
targetFps=10.0
```

## 依赖

- Java 21
- [OpenCV](https://opencv.org/) - 视频处理
- [owner](https://github.com/lviggiano/owner) - 配置管理

## 许可证

MIT License