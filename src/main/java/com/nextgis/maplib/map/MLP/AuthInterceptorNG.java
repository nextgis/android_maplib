package com.nextgis.maplib.map.MLP;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        if (originalRequest.url().toString().contains("quadtiles")) {
            // Q scheme

            String keyword = "quadtiles";
            String url = originalRequest.url().toString();

            int idx = url.indexOf(keyword);
            int start = idx + keyword.length();
            int dotIndex = url.indexOf('.', start);
            String coordsPart = url.substring(start, dotIndex);
            if (coordsPart.startsWith("/")) {
                coordsPart = coordsPart.substring(1);
            }

            String[] parts = coordsPart.split("/");

            if (parts.length == 3) {
                int z = Integer.parseInt(parts[0]);
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);

                String replacement = tileXYToQuadKey(x, y, z);
                String newUrl = url.substring(0, idx) + replacement + url.substring(dotIndex);
                System.out.println(newUrl);

                Request newRequest = originalRequest
                        .newBuilder()
                        .url(newUrl)
                        .build();
                return chain.proceed(newRequest);
            }
        }

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

    private String tileXYToQuadKey(int x, int y, int z) {
        StringBuilder quadKey = new StringBuilder();
        for (int i = z; i > 0; i--) {
            int digit = 0;
            int mask = 1 << (i - 1);
            if ((x & mask) != 0) digit++;
            if ((y & mask) != 0) digit += 2;
            quadKey.append(digit);
        }
        return quadKey.toString();
    }
}