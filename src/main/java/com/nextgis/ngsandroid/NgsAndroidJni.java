package com.nextgis.ngsandroid;


import android.graphics.Bitmap;


public class NgsAndroidJni
{
    public native static boolean initLogger();


    public native static long lockBitmapPixels(Bitmap bitmap);


    public native static void unlockBitmapPixels(Bitmap bitmap);


    public native static boolean fillImage(
            long imagePointer,
            int imageWidth,
            int imageHeight);


    static {
        try {
            System.loadLibrary("ngsandroid");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load. \n" + e);
            System.exit(1);
        }
    }
}
