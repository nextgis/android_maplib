# File Application.mk

NDK_TOOLCHAIN_VERSION := 4.9
APP_STL := gnustl_static

# drop mips for now: mips  mips64
APP_ABI := armeabi armeabi-v7a x86 arm64-v8a x86_64
#APP_ABI := armeabi-v7a # for debug

#  Enable C++11. However, pthread, rtti and exceptions aren’t enabled
APP_CPPFLAGS += -std=c++11

# minimum API 15
# APP_ABI := armeabi armeabi-v7a
# APP_PLATFORM := android-8
