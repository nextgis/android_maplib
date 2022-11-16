package com.nextgis.maplib.util;

import java.net.HttpURLConnection;

public class ExistFeatureResult {
    public final Object object;
    public final boolean result;
    public final int code;

    public  ExistFeatureResult(final Object object,final boolean result ,  final int code){
        this.object = object;
        this.result = result;
        this.code = code;

    }
}
