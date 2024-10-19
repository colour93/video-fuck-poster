#include <android/log.h>
#include <iostream>
#include <fstream>

#define LOG_TAG "FFmpegJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C" {
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavutil/avutil.h"
#include "libswscale/swscale.h"
#include "libavutil/imgutils.h"
}

// BMP 文件头的定义
#pragma pack(push, 1)  // 保证结构体内的成员是按1字节对齐
struct BMPFileHeader {
    uint16_t bfType = 0x4D42;  // 文件类型 ("BM")
    uint32_t bfSize;           // 文件大小
    uint16_t bfReserved1 = 0;  // 保留字段
    uint16_t bfReserved2 = 0;  // 保留字段
    uint32_t bfOffBits = 54;   // 像素数据的偏移量 (54 字节)
};

struct BMPInfoHeader {
    uint32_t biSize = 40;          // 信息头大小 (40 字节)
    int32_t biWidth;               // 图像宽度
    int32_t biHeight;              // 图像高度
    uint16_t biPlanes = 1;         // 平面数（必须为1）
    uint16_t biBitCount = 24;      // 每个像素的位数（24位，用于RGB）
    uint32_t biCompression = 0;    // 压缩类型（0表示不压缩）
    uint32_t biSizeImage = 0;      // 图像大小（可以设置为0，不压缩）
    int32_t biXPelsPerMeter = 0;   // 水平分辨率（像素/米）
    int32_t biYPelsPerMeter = 0;   // 垂直分辨率（像素/米）
    uint32_t biClrUsed = 0;        // 调色板颜色数（0表示不使用调色板）
    uint32_t biClrImportant = 0;   // 重要颜色数（0表示所有颜色都重要）
};
#pragma pack(pop)

void save_frame_as_ppm(AVFrame *pFrame, int width, int height, const char *outputPath) {
    std::ofstream outFile(outputPath, std::ios::binary);
    outFile << "P6\n" << width << " " << height << "\n255\n";

    for (int y = 0; y < height; y++) {
        outFile.write(reinterpret_cast<char*>(pFrame->data[0] + y * pFrame->linesize[0]), width * 3);
    }

    outFile.close();
}

void save_frame_as_bmp(AVFrame *pFrame, int width, int height, const char *outputPath) {
    // 打开输出文件
    std::ofstream outFile(outputPath, std::ios::binary);

    if (!outFile.is_open()) {
        std::cerr << "Could not open output file: " << outputPath << std::endl;
        return;
    }

    // 创建 BMP 文件头和信息头
    BMPFileHeader fileHeader;
    BMPInfoHeader infoHeader;

    // 设置图像的宽度和高度（高度为负表示从上到下的顺序存储）
    infoHeader.biWidth = width;
    infoHeader.biHeight = -height;  // BMP 文件从左下角开始绘制，所以高度取负，表示从上到下存储

    // 每行像素的字节数，必须是4的倍数
    int rowSize = (width * 3 + 3) & (~3);

    // 计算文件大小（头 + 像素数据）
    fileHeader.bfSize = sizeof(BMPFileHeader) + sizeof(BMPInfoHeader) + rowSize * height;

    // 写入 BMP 文件头
    outFile.write(reinterpret_cast<const char*>(&fileHeader), sizeof(fileHeader));

    // 写入 BMP 信息头
    outFile.write(reinterpret_cast<const char*>(&infoHeader), sizeof(infoHeader));

    // 写入像素数据 (行填充到 4 字节对齐)
    uint8_t padding[3] = {0, 0, 0};  // 用于填充的字节

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            // 获取 RGB 数据并转换为 BGR
            uint8_t r = pFrame->data[0][y * pFrame->linesize[0] + x * 3 + 0];
            uint8_t g = pFrame->data[0][y * pFrame->linesize[0] + x * 3 + 1];
            uint8_t b = pFrame->data[0][y * pFrame->linesize[0] + x * 3 + 2];

            outFile.put(b); // 写入 B
            outFile.put(g); // 写入 G
            outFile.put(r); // 写入 R
        }

        // 如果一行数据不是4的倍数，填充空字节
        outFile.write(reinterpret_cast<const char*>(padding), rowSize - width * 3);
    }

    outFile.close();
    std::cout << "Saved BMP file: " << outputPath << std::endl;
}
