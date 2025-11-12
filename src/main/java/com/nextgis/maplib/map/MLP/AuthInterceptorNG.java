package com.nextgis.maplib.map.MLP;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AuthInterceptorNG implements Interceptor {

    List<String[]> accounts = new ArrayList<>();

    public void addAuth(String[] auth) {
        accounts.add(auth);
    }

    public void removeAuth(String[] ids) {
        //accounts.remove(id);
    }


    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        for (String[] part : accounts) {
            if (originalRequest.url().toString().contains(part[0])
            && originalRequest.url().toString().contains(part[1])) {

                Log.d("AuthInterceptor", "Adding Authorization to: " + originalRequest.url());
                Request newRequest = originalRequest.newBuilder()
                        .addHeader("Authorization", part[2])
                        .build();
                return chain.proceed(newRequest);
            }
        }
        return chain.proceed(originalRequest);
    }
}