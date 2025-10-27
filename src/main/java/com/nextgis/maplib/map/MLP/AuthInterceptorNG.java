package com.nextgis.maplib.map.MLP;

import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

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

//        if (originalRequest.url().toString().contains("alexey655.nextgis.com")
//                && originalRequest.url().toString().contains("resource=316")) {
//            Log.d("AuthInterceptor", "Adding Authorization to: " + originalRequest.url());
//            Request newRequest = originalRequest.newBuilder()
//                    .addHeader("Authorization", "Basic YWxleGV5NjU1OlJqa2osanI0Jg==")
//                    .build();
//            return chain.proceed(newRequest);
//        }

        // Фильтруем по URL, чтобы добавить заголовок только для тайлов
//    }
//}