package com.nextgis.store;

/**
 * Created by bishop on 11.05.16.
 */
public class ngstore {

    static {
        System.loadLibrary("ngstore");
        System.loadLibrary("ngstoreapi");
    }

    public static String reportVersionString(){
        return api.GetVersionString();
    }

    public static int reportVersion() {
        return api.GetVersion();
    }

}
