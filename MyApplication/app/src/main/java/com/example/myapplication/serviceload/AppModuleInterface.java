package com.example.myapplication.serviceload;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.example.annotation.ServiceLoaderInterface;
import com.example.sample_baseinterface.ModuleInterface;
import com.example.serviceloader.ServiceLoader;

@ServiceLoaderInterface(key = "app", interfaceClass = ModuleInterface.class)
public class AppModuleInterface implements ModuleInterface {
    @Override
    public boolean isHandleCate(Context context, String searchword, Bundle bundle) {
        Log.i(ServiceLoader.TAG,"-------isHandleCate------"+searchword);
        return false;
    }

    @Override
    public Fragment getFragment(Context context, String searchword, Bundle bundle) {
        Log.i(ServiceLoader.TAG,"-------getFragment------"+searchword);
        return null;
    }

    @Override
    public boolean isHandle(boolean handle) {
        return  handle;
    }
}
