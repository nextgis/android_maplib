package com.nextgis.ngsandroid;


import android.graphics.Bitmap;

import java.nio.ByteBuffer;


public class NgsAndroidJni
{
    public native static boolean initLogger();


    public native static long lockBitmapPixels(Bitmap bitmap);


    public native static void unlockBitmapPixels(Bitmap bitmap);


    public native static Bitmap fillBitmapFromBuffer(
            ByteBuffer buffer,
            int width,
            int height);


    static {
        try {
            System.loadLibrary("ngsandroid");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load. \n" + e);
            System.exit(1);
        }
    }
}
