/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import org.gradle.internal.os.OperatingSystem;


apply plugin: 'com.android.library'


def getNdkPlatformLevel(String abi)
{
    def platform32 = 9
    def platform64 = 21

    switch (abi) {
        case "armeabi":
            return platform32
        case "armeabi-v7a":
            return platform32
        case "x86":
            return platform32
        case "mips":
            return platform32
        default:
            return platform32

        case "arm64-v8a":
            return platform64
        case "x86_64":
            return platform64
        case "mips64":
            return platform64
    }
}


def buildTypeName = ""


android {
    compileSdkVersion 23
    buildToolsVersion '23.0.3'

    defaultConfig {
        minSdkVersion 9
        targetSdkVersion 23
        versionCode = 19
        versionName = '3.0'
    }

    buildTypes {
        release {
            buildTypeName = 'Release'
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
        debug {
            buildTypeName = 'Debug'
            jniDebuggable true
        }
    }

    productFlavors {
    }
}


// native part
android {
    // drop mips for now "mips", , "mips64"
//    def androidAbis = ["armeabi", "armeabi-v7a", "x86", "arm64-v8a", "x86_64"]
    def androidAbis = ["armeabi-v7a"] // TODO:
    def abiFiltersString = androidAbis.toString()
    abiFiltersString = abiFiltersString.substring(1, abiFiltersString.length() - 1)

    def cmakeSdkExe = "${getSdkDir()}/cmake/bin/cmake"
    def cmakeInstDir = "${buildDir}/cpp/inst"

    defaultConfig {
        cmake {
            abiFilters "${abiFiltersString}"

            // targets /*"ngstore",*/ "install" // TODO: for AS 2.2 preview 4

            arguments "-DCMAKE_INSTALL_PREFIX=${cmakeInstDir}/",

                    "-DSUPRESS_VERBOSE_OUTPUT=OFF",
//                    "-DCMAKE_VERBOSE_MAKEFILE=TRUE",

                    "-DBUILD_TARGET_PLATFORM=ANDROID",

                    "-DANDROID_NATIVE_API_LEVEL=9", // ${getNdkPlatformLevel(abi)}
                    "-DANDROID_STL=gnustl_static",
                    "-DANDROID_TOOLCHAIN_NAME=arm-linux-androideabi-4.9",
                    "-GAndroid Gradle - Unix Makefiles",
                    "-DCMAKE_MAKE_PROGRAM=make",

                    "-DCXX_STANDARD=14",
                    "-DCXX_STANDARD_REQUIRED=ON",

//                    "-DCMAKE_BUILD_TYPE=Release" // let's always release ${buildTypeName}
                    "-DCMAKE_BUILD_TYPE=Debug" // TODO:

//                    "-DANDROID_ABI=${abi}",
//                    "-DBUILD_SHARED_LIBS=ON",

            // include flags for code editor
            cFlags   "-I ${cmakeInstDir}}/include"
            cppFlags "-I ${cmakeInstDir}/include"
        }
    }
    externalNativeBuild {
        cmake {
            path "libngstore/CMakeLists.txt"
        }
    }
    sourceSets {
        main {
            // let gradle pack the shared library into apk
// TODO:
//            jniLibs.srcDirs = ["${cmakeInstDir}"]
//            jniLibs.srcDir 'src/main/libs' //set libs as .so's location instead of jniLibs
            //jni.srcDirs = []  // disable automatic ndk-build, if you do not have
            // src/main/jni directory at all, no need for it
        }
    }

    task "cmakeInstall"(type: Exec) {
        task ->
            description 'Execute cmake install.'
            workingDir "${projectDir}/build/intermediates/cmake/debug/json/armeabi-v7a" // TODO: set from buildType

            def cmakeCmd = "${cmakeSdkExe} --build . --target install"
            commandLine getShell(), getShellArg(), "${cmakeCmd}"
            println("Command line of ${task.getName()}: ${commandLine}")
    }

    task copyJSources(type: Copy, dependsOn: cmakeInstall) {
        description 'Copy java files.'

        from(new File("${cmakeInstDir}", "/src")) { include '**/*.java' }
        into new File("src/main/java/com/nextgis/store")
    }

    cmakeInstall.dependsOn {
        tasks.findAll { task -> task.name.equals("externalNativeBuildDebug") }
    }

// TODO:
//    androidAbis.each { androidAbi ->
//        task "cleanNative-${androidAbi}"(type: Exec) {
//            workingDir getWorkDir(androidAbi)
//            def cmakeCmd = "cmake --build . --target clean"
//            commandLine getShell(), getShellArg(), "${cmakeCmd}"
//        }
//    }
//    clean.dependsOn androidAbis.collect { androidAbi -> "cleanNative-${androidAbi}" }
}

tasks.all {
    task ->
        if (task.name.equals("compileDebugSources")) { // TODO: set from buildType
            task.dependsOn copyJSources
        }

        if (task.name.equals("externalNativeBuildRelease")) { // TODO: set from !buildType
            task.enabled = false
        }
}


dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
}


def getSdkDir()
{
    if (System.env.ANDROID_HOME != null) {
        return System.env.ANDROID_HOME
    }

    Properties properties = new Properties()
    properties.load(project.rootProject.file('local.properties').newDataInputStream())
    def sdkdir = properties.getProperty('sdk.dir', null)
    if (sdkdir == null) {
        throw new GradleException("""\
                SDK location not found.
                Define location with sdk.dir in the local.properties file
                or with an ANDROID_HOME environment variable.""")
    }

    return sdkdir
}


def getNdkDir()
{
    if (System.env.ANDROID_NDK_ROOT != null) {
        return System.env.ANDROID_NDK_ROOT
    }

    Properties properties = new Properties()
    properties.load(project.rootProject.file('local.properties').newDataInputStream())
    def ndkdir = properties.getProperty('ndk.dir', null)
    if (ndkdir == null) {
        throw new GradleException("""\
                NDK location not found.
                Define location with ndk.dir in the local.properties file
                or with an ANDROID_NDK_ROOT environment variable.""")
    }

    return ndkdir
}


def getNdkBuildCmd()
{
    def ndkbuild = getNdkDir() + "/ndk-build"
    if (OperatingSystem.current().isWindows()) {
        ndkbuild += ".cmd"
    }
    return ndkbuild
}


def getShell()
{
    if (OperatingSystem.current().isWindows()) {
        return "cmd"
    } else {
        return "sh"
    }
}


def getShellArg()
{
    if (OperatingSystem.current().isWindows()) {
        return "/c"
    } else {
        return "-c"
    }
}


def getWorkDir(String abi)
{
    def libngstorefolder = 'libngstore'
    def folder = project.file("${libngstorefolder}/build-${abi}/inst")
    if (!folder.exists()) {
        // Create all folders up-to and including build
        folder.mkdirs()
    }

    return folder.parent
}
