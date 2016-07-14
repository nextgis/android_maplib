# File: Android.mk
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := ngstore
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libngstore.so
#LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include/ngstore
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := ngstoreapi
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libngstoreapi.so
#LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include/ngstoreapi
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := ngsandroid
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libngsandroid.so
#LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include/ngsandroid
include $(PREBUILT_SHARED_LIBRARY)
