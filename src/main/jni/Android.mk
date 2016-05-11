# File: Android.mk
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := ngstore
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libngstore.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include/ngstore
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := ngstoreapi
LOCAL_SRC_FILES := src/api_wrap.cpp
LOCAL_SHARED_LIBRARIES := ngstore
include $(BUILD_SHARED_LIBRARY)
