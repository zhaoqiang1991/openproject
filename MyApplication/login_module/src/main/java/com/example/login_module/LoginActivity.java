package com.example.login_module;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class LoginActivity extends AppCompatActivity {
    public static final int WHAT = 0x1;
    private LoginImpl loginImpl = LoginImpl.LoginImplFactory.getLoginImpl();
    private Context context;

    private WeakHandler handler;


    private class WeakHandler extends Handler {
        private WeakReference<Context> ref;

        public WeakHandler(WeakReference<Context> ref) {
            this.ref = ref;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (ref.get() == null) return;
            if (msg != null && msg.what == WHAT) {
                Toast.makeText(ref.get(), "登陆成功了", Toast.LENGTH_LONG).show();
                loginImpl.setContext(LoginActivity.this);
                //登陆成功跳转到首页
                loginImpl.loginSuccess();
            }


        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        context = this;
        handler = new WeakHandler(new WeakReference<Context>(context));


        Button btn = this.findViewById(R.id.login_btn);
        handler.sendEmptyMessageDelayed(Message.obtain().what = WHAT, 1000);
        Toast.makeText(context, "开始登陆了", Toast.LENGTH_LONG).show();


    }
}
