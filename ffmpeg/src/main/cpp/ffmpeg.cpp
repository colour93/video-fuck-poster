#include <jni.h>
#include <string>
#include <sstream>
#include <cstdlib>
#include <unistd.h>
#include <iostream>
#include <fstream>
#include <android/log.h>
#include <nlohmann/json.hpp>

using json = nlohmann::json;

extern "C" {
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavutil/avutil.h"
#include "libswscale/swscale.h"
#include "libavutil/imgutils.h"
}

#include "utils.h"

#define LOG_TAG "FFmpegJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jstring JNICALL
Java_icu_fur93_ffmpeg_FFmpegJni_getVersion(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(av_version_info());
}

// 获取视频文件的信息
extern "C"
JNIEXPORT jstring JNICALL
Java_icu_fur93_ffmpeg_FFmpegJni_getVideoInfo(JNIEnv *env, jobject thiz, jstring filePath) {
    const char *inputFile = env->GetStringUTFChars(filePath, nullptr);

    AVFormatContext *fmt_ctx = avformat_alloc_context();
    if (!fmt_ctx) {
        env->ReleaseStringUTFChars(filePath, inputFile);
        return env->NewStringUTF("Failed to allocate AVFormatContext");
    }

    if (avformat_open_input(&fmt_ctx, inputFile, nullptr, nullptr) < 0) {
        env->ReleaseStringUTFChars(filePath, inputFile);
        avformat_free_context(fmt_ctx);
        return env->NewStringUTF("Failed to open video file");
    }

    if (avformat_find_stream_info(fmt_ctx, nullptr) < 0) {
        avformat_close_input(&fmt_ctx);
        env->ReleaseStringUTFChars(filePath, inputFile);
        return env->NewStringUTF("Failed to retrieve stream info");
    }

    // 创建一个 JSON 对象
    json videoInfoJson;

    // 添加格式信息
    videoInfoJson["path"] = env->GetStringUTFChars(filePath, nullptr);
    videoInfoJson["format"] = fmt_ctx->iformat->name;
    videoInfoJson["duration"] = fmt_ctx->duration / AV_TIME_BASE; // 秒
    videoInfoJson["bitrate"] = fmt_ctx->bit_rate;
    videoInfoJson["numStreams"] = fmt_ctx->nb_streams;

    // 遍历每个流，提取视频流信息
    for (unsigned int i = 0; i < fmt_ctx->nb_streams; i++) {
        AVStream *stream = fmt_ctx->streams[i];
        AVCodecParameters *codecpar = stream->codecpar;

        if (codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            json videoStream;
            videoStream["codec"] = avcodec_get_name(codecpar->codec_id);
            videoStream["resolution"] = {{"width",  codecpar->width},
                                         {"height", codecpar->height}};
            videoStream["frameRate"] = av_q2d(stream->avg_frame_rate);
            videoInfoJson["videoStreams"].push_back(videoStream);
        } else if (codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            json audioStream;
            audioStream["codec"] = avcodec_get_name(codecpar->codec_id);
            audioStream["sampleRate"] = codecpar->sample_rate;
            audioStream["channels"] = codecpar->ch_layout.nb_channels;
            videoInfoJson["audioStreams"].push_back(audioStream);
        }
    }

    avformat_close_input(&fmt_ctx);
    env->ReleaseStringUTFChars(filePath, inputFile);

    // 返回 JSON 字符串
    return env->NewStringUTF(videoInfoJson.dump().c_str());
}


// 获取视频截图
extern "C"
JNIEXPORT jboolean JNICALL
Java_icu_fur93_ffmpeg_FFmpegJni_captureFrame(JNIEnv *env, jobject obj, jstring jVideoPath,
                                             jfloat timeInSeconds, jstring jOutputPath) {
    const char *videoPath = env->GetStringUTFChars(jVideoPath, nullptr);
    const char *outputPath = env->GetStringUTFChars(jOutputPath, nullptr);

    AVFormatContext *pFormatContext = nullptr;
    AVCodecContext *pCodecContext = nullptr;
    AVFrame *pFrame = nullptr, *pFrameRGB = nullptr;
    AVPacket packet;
    struct SwsContext *swsCtx = nullptr;

    int videoStreamIndex = -1;
    int response = 0;
    int targetFrameIndex = 0;
    float fps = 0;

    // 打开视频文件
    if (avformat_open_input(&pFormatContext, videoPath, nullptr, nullptr) != 0) {
        LOGD("Could not open video file: %s", videoPath);
        return JNI_FALSE;
    }

    // 获取视频文件信息
    if (avformat_find_stream_info(pFormatContext, nullptr) < 0) {
        LOGD("Could not find stream information");
        return JNI_FALSE;
    }

    // 查找视频流
    for (int i = 0; i < pFormatContext->nb_streams; i++) {
        if (pFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoStreamIndex = i;
            break;
        }
    }
    if (videoStreamIndex == -1) {
        LOGD("Could not find video stream");
        return JNI_FALSE;
    }

    // 获取视频帧率
    AVStream *videoStream = pFormatContext->streams[videoStreamIndex];
    fps = av_q2d(videoStream->r_frame_rate);
    if (fps <= 0) {
        LOGD("Could not determine FPS");
        return JNI_FALSE;
    }

    // 计算目标帧索引
    targetFrameIndex = (int) (timeInSeconds * fps);

    // 获取解码器
    AVCodecParameters *pCodecParameters = pFormatContext->streams[videoStreamIndex]->codecpar;
    const AVCodec *pCodec = avcodec_find_decoder(pCodecParameters->codec_id);
    if (pCodec == nullptr) {
        LOGD("Unsupported codec!");
        return JNI_FALSE;
    }

    pCodecContext = avcodec_alloc_context3(pCodec);
    if (avcodec_parameters_to_context(pCodecContext, pCodecParameters) < 0) {
        LOGD("Couldn't copy codec context");
        return JNI_FALSE;
    }

    // 打开解码器
    if (avcodec_open2(pCodecContext, pCodec, nullptr) < 0) {
        LOGD("Could not open codec");
        return JNI_FALSE;
    }

    // 分配帧内存
    pFrame = av_frame_alloc();
    pFrameRGB = av_frame_alloc();
    if (pFrame == nullptr || pFrameRGB == nullptr) {
        LOGD("Could not allocate frame");
        return JNI_FALSE;
    }

    // 分配 RGB 图像内存
    int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGB24, pCodecContext->width,
                                            pCodecContext->height, 32);
    uint8_t *buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
    av_image_fill_arrays(pFrameRGB->data, pFrameRGB->linesize, buffer, AV_PIX_FMT_RGB24,
                         pCodecContext->width, pCodecContext->height, 1);

    // 初始化缩放上下文
    swsCtx = sws_getContext(pCodecContext->width, pCodecContext->height, pCodecContext->pix_fmt,
                            pCodecContext->width, pCodecContext->height, AV_PIX_FMT_RGB24,
                            SWS_BILINEAR, nullptr, nullptr, nullptr);

    // 读取帧，直到找到目标帧
    int currentFrameIndex = 0;
    bool frameCaptured = false;

    while (av_read_frame(pFormatContext, &packet) >= 0) {
        if (packet.stream_index == videoStreamIndex) {
            response = avcodec_send_packet(pCodecContext, &packet);
            if (response < 0) {
                LOGD("Error while sending packet to decoder");
                break;
            }

            while (response >= 0) {
                response = avcodec_receive_frame(pCodecContext, pFrame);
                if (response == AVERROR(EAGAIN) || response == AVERROR_EOF) {
                    break;  // 返回外层循环，读取下一个包
                } else if (response < 0) {
                    LOGD("Error while receiving frame from decoder");
                    return JNI_FALSE;
                }

                // 如果当前帧是目标帧
                if (currentFrameIndex == targetFrameIndex) {
                    // 将图像转换为 RGB
                    sws_scale(swsCtx, (uint8_t const *const *) pFrame->data,
                              pFrame->linesize, 0, pCodecContext->height,
                              pFrameRGB->data, pFrameRGB->linesize);

                    // 保存帧到文件
                    save_frame_as_bmp(pFrameRGB, pCodecContext->width, pCodecContext->height,
                                      outputPath);
                    LOGD("Frame saved to %s", outputPath);

                    frameCaptured = true;
                    break;
                }

                currentFrameIndex++;
            }

            if (frameCaptured) {
                break;  // 找到目标帧后，跳出整个循环
            }
        }
        av_packet_unref(&packet);
    }

    // 释放内存
    av_free(buffer);
    av_frame_free(&pFrame);
    av_frame_free(&pFrameRGB);
    avcodec_free_context(&pCodecContext);
    avformat_close_input(&pFormatContext);

    env->ReleaseStringUTFChars(jVideoPath, videoPath);
    env->ReleaseStringUTFChars(jOutputPath, outputPath);

    return frameCaptured ? JNI_TRUE : JNI_FALSE;
}