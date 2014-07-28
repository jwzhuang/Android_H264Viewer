/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include "H264Decoder.h"
#include <string.h>
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <android/log.h>

#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>

#define  LOG_TAG    "Decoder"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

AVCodec *codec;
AVCodecContext *c = NULL;
int got_picture, len;
AVFrame *picture;
AVFrame *rgbframe;
AVPacket avpkt;
struct SwsContext *img_convert_ctx = NULL;

JNIEXPORT jint JNICALL Java_tw_jwzhuang_ipcamviewer_h264_Decoder_Init(JNIEnv* env, jobject thiz) {
	avcodec_register_all();
	av_init_packet(&avpkt);

	codec = avcodec_find_decoder(CODEC_ID_H264);
	if (!codec) {
		LOGE("codec not found");
		return -1;
	}

	c = avcodec_alloc_context3(codec);
	picture = avcodec_alloc_frame();

	if (codec->capabilities & CODEC_CAP_TRUNCATED)
		c->flags |= CODEC_FLAG_TRUNCATED; /* we do not send complete frames */

	/* For some codecs, such as msmpeg4 and mpeg4, width and height
	 MUST be initialized there because this information is not
	 available in the bitstream. */

	/* open it */
	if (avcodec_open2(c, codec, NULL) < 0) {
		fprintf(stderr, "could not open codec\n");
		return -1;
	}
	return 0;
}

JNIEXPORT jint JNICALL Java_tw_jwzhuang_ipcamviewer_h264_Decoder_free(JNIEnv* env,
		jobject thiz) {
	avcodec_close(c);
	av_free(c);
	av_free(picture);
}

JNIEXPORT jint JNICALL Java_tw_jwzhuang_ipcamviewer_h264_Decoder_decodeFrame(
		JNIEnv* env, jobject thiz, const jbyteArray pSrcData,
		const jint dwDataLen, const jbyteArray pDeData) {

	jbyte * Buf = (jbyte*) (*env)->GetByteArrayElements(env, pSrcData, 0);
	jbyte * Pixel = (jbyte*) (*env)->GetByteArrayElements(env, pDeData, 0);

	avpkt.data = Buf;
	avpkt.size = dwDataLen;
	len = avcodec_decode_video2(c, picture, &got_picture, &avpkt);
	if (got_picture) {
		rgbframe = avcodec_alloc_frame();
		avpicture_fill((AVPicture *) rgbframe, (uint8_t *) Pixel, PIX_FMT_RGB565,
				c->width, c->height);
		img_convert_ctx = sws_getContext(c->width, c->height, c->pix_fmt,
				c->width, c->height, PIX_FMT_RGB565, SWS_BICUBIC, NULL, NULL, NULL);
		if (NULL != c) {
			sws_scale(img_convert_ctx,
					(const uint8_t* const *) picture->data,
					picture->linesize, 0, c->height, rgbframe->data,
					rgbframe->linesize);
		}
		av_free(rgbframe);
		sws_freeContext(img_convert_ctx);
	}
	(*env)->ReleaseByteArrayElements(env, pSrcData, Buf, 0);
	(*env)->ReleaseByteArrayElements(env, pDeData, Pixel, 0);
	return len;
}
