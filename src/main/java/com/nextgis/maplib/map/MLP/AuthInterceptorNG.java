package com.nextgis.maplib.map.MLP;

import android.util.Log;
import java.io.IOException;
import java.util.HashMap;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptorNG implements Interceptor {

    HashMap<String, String[]> accounts = new HashMap<>();


    public void addAuth(String id, String[] auth) {
        accounts.put(id, auth);
    }

    public void removeAuth(String id) {
        accounts.remove(id);
    }


    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        for (String part : accounts.keySet()) {
            if (originalRequest.url().toString().contains(part)
            && originalRequest.url().toString().contains( accounts.get(part)[0])) {

                Log.d("AuthInterceptor", "Adding Authorization to: " + originalRequest.url());
                Request newRequest = originalRequest.newBuilder()
                        .addHeader("Authorization", accounts.get(part)[1])
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