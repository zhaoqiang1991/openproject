package com.example.home_module.serviceimpl;

import android.content.Context;
import android.content.Intent;

import com.example.annotation.ServiceLoaderInterface;
import com.example.home_module.HomeActivity;
import com.example.home_module_api.IHomeService;

@ServiceLoaderInterface(key = "home", interfaceClass = IHomeService.class)
public class HomeServiceImpl implements IHomeService {
    @Override
    public void startHomeActivity(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }
}
