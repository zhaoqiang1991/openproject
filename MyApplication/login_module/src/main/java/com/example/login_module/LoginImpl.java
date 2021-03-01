package com.example.login_module;

import android.content.Context;
import android.content.Intent;

import com.example.annotation.ServiceLoaderInterface;
import com.example.home_module_api.IHomeService;
import com.example.login_module_api.ILoginService;
import com.example.serviceloader.ServiceLoader;

@ServiceLoaderInterface(key = "login", interfaceClass = ILoginService.class)
public class LoginImpl implements ILoginService {

    private Context context;

    public static class LoginImplFactory {
        private LoginImplFactory() {
        }

        public static LoginImpl getLoginImpl() {
            return new LoginImpl();
        }
    }

    public void setContext(Context context) {
        this.context = context;
    }

    //创建服务
    private IHomeService homeService = ServiceLoader.load(IHomeService.class, null).get(0);

    @Override
    public void login(Context context) {
        Intent intent = new Intent(context,LoginActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void loginSuccess() {
        //登陆成功跳转到首页
        if (homeService != null) {
            homeService.startHomeActivity(context);
        }
    }
}
