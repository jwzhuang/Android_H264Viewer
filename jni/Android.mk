# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)

#include $(CLEAR_VARS)
#LOCAL_MODULE    := avfilter
#LOCAL_SRC_FILES := libavfilter.a  #这几个prebuild是为了把库预编译一下，ndk会将它移动到libs目录下面去
#include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := avutil
LOCAL_SRC_FILES := libavutil.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := avcodec
LOCAL_SRC_FILES := libavcodec.a
include $(PREBUILT_STATIC_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE    := avdevice
#LOCAL_SRC_FILES := libavdevice.a
#include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := swscale
LOCAL_SRC_FILES := libswscale.a
include $(PREBUILT_STATIC_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE    := swresample
#LOCAL_SRC_FILES := libswresample.a
#include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
#ifeq ($(TARGET_ARCH),armeabi)
#    LOCAL_CFLAGS   := -march=armv6 -mfpu=vfp -mfloat-abi=softfp
#else
LOCAL_CFLAGS := -D__STDC_CONSTANT_MACROS -Wno-sign-compare -Wno-switch -march=armv7-a -Wno-pointer-sign -DHAVE_NEON=1 -mfpu=neon -mfloat-abi=softfp -fPIC -DANDROID
#endif
LOCAL_MODULE    := H264Decoder
LOCAL_SRC_FILES := H264Decoder.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_LDLIBS :=-L$(NDK_PLATFORMS_ROOT)/$(TARGET_PLATFORM)/arch-arm/usr/lib -L$(LOCAL_PATH) -lavcodec -lavutil -lswscale -llog -ljnigraphics -lz -ldl -lgcc
include $(BUILD_SHARED_LIBRARY)
