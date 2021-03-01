package com.example.myapplication.serviceload;

import android.app.Application;
import android.util.Log;

import com.example.serviceloader.ServiceLoader;

public class SimpleApplication extends Application {
    public static final String TAG = "lh";
    @Override
    public void onCreate() {
        super.onCreate();
        ServiceLoader.asyncInit(this, new ServiceLoader.OnErrorListener() {
            @Override
            public void onError(Exception e) {
                Log.i(ServiceLoader.TAG,"service load加载失败！！！"+e.getStackTrace());
            }
        });
    }
}
