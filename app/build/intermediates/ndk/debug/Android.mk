LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := FFT
LOCAL_SRC_FILES := \
	/Users/swu/AndroidStudioProjects/pitchDetector/app/src/main/jni/Android.mk \
	/Users/swu/AndroidStudioProjects/pitchDetector/app/src/main/jni/processRawData.c \

LOCAL_C_INCLUDES += /Users/swu/AndroidStudioProjects/pitchDetector/app/src/main/jni
LOCAL_C_INCLUDES += /Users/swu/AndroidStudioProjects/pitchDetector/app/src/debug/jni

include $(BUILD_SHARED_LIBRARY)
