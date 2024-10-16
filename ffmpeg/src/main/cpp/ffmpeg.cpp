#include <jni.h>
#include <string>
#include <sstream>
#include <cstdlib>
#include <unistd.h>
#include <nlohmann/json.hpp>
using json = nlohmann::json;

extern "C" {
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavutil/avutil.h"
}

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
            videoStream["resolution"] = {{"width", codecpar->width}, {"height", codecpar->height}};
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
JNIEXPORT jstring JNICALL
Java_icu_fur93_ffmpeg_FFmpegJni_captureFrame(JNIEnv *env, jobject thiz, jstring filePath, jstring outputImagePath, jstring time) {
    const char *inputFile = env->GetStringUTFChars(filePath, nullptr);
    const char *outputFile = env->GetStringUTFChars(outputImagePath, nullptr);
    const char *captureTime = env->GetStringUTFChars(time, nullptr);

    // FFmpeg 命令
    std::string command = "ffmpeg -i ";
    command += inputFile;
    command += " -ss ";
    command += captureTime;
    command += " -frames:v 1 ";
    command += outputFile;

    // 检查输出路径是否可写
    if (access(outputFile, W_OK) != 0) {
        env->ReleaseStringUTFChars(filePath, inputFile);
        env->ReleaseStringUTFChars(outputImagePath, outputFile);
        env->ReleaseStringUTFChars(time, captureTime);
        return env->NewStringUTF("Output path is not writable");
    }

    // 执行 FFmpeg 命令
    int ret = system(command.c_str());

    env->ReleaseStringUTFChars(filePath, inputFile);
    env->ReleaseStringUTFChars(outputImagePath, outputFile);
    env->ReleaseStringUTFChars(time, captureTime);

    // 根据 FFmpeg 命令执行的返回值返回结果
    if (ret == 0) {
        return env->NewStringUTF("success");
    } else {
        return env->NewStringUTF("failed");
    }
}