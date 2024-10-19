#ifndef VIDEO_FUCK_POSTER_UTILS_H
#define VIDEO_FUCK_POSTER_UTILS_H

#include "libswscale/swscale.h"

double r2d(AVRational r);

void save_frame_as_ppm(AVFrame *pFrame, int width, int height, const char *filename);

void save_frame_as_bmp(AVFrame *pFrame, int width, int height, const char *out_file);

#endif //VIDEO_FUCK_POSTER_UTILS_H
