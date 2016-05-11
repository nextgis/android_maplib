# File Application.mk
NDK_TOOLCHAIN_VERSION := 4.9
# drop mips for now: mips  mips64
APP_ABI := armeabi armeabi-v7a x86 arm64-v8a x86_64
#  Enable C++11. However, pthread, rtti and exceptions aren’t enabled 
APP_CPPFLAGS += -std=c++11
# Instruct to use the static GNU STL implementation
APP_STL := gnustl_static
LOCAL_C_INCLUDES += ${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++/4.9/include
# minimum API 15
# APP_ABI := armeabi armeabi-v7a
# APP_PLATFORM := android-8
