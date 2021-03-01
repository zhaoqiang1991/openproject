package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.buniffer.ButterKnife;
import com.example.butter_annotation.BindView;
import com.example.login_module_api.ILoginService;
import com.example.myapplication.day20210225.MainEventActivity;
import com.example.myapplication.day3.People;
import com.example.myapplication.day3.animation.RequestType;
import com.example.myapplication.serviceload.MainActivity;
import com.example.rxjava1_module.Fun1;
import com.example.rxjava1_module.Observer;
import com.example.rxjava1_module.OnSubscribe;
import com.example.rxjava1_module.Subscribe;
import com.example.serviceloader.ServiceLoader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Main2Activity extends AppCompatActivity {

    @BindView(R.id.tv)
    TextView textView;

    @BindView(R.id.btn)
    Button button;

    @BindView(R.id.start_serviceActivity_btn)
    Button startServiceActivity;

    @BindView(R.id.start_login)
    Button startLogin;

    @BindView(R.id.start_main3)
    Button startMain3;

    @BindView(R.id.start_main4)
    Button startMain4;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
       /* DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        View view = getWindow().getDecorView();
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams)view.getLayoutParams();
        lp.gravity = Gravity.BOTTOM;
        lp.verticalMargin = 300;
        //不设置宽高就是xml布局的宽高
//      lp.width = (dm.widthPixels * 4) / 5;
//      lp.height = (dm.widthPixels * 4) / 5;
        getWindowManager().updateViewLayout(view,lp);
        //下面两行代码的顺序不可以改变不然圆角背景就设置不上了
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        view.setBackgroundResource(R.drawable.popup_window);//圆角背景*/
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        ButterKnife.bind(this);

        textView.setText("测试");
        String str = "点击我跳转点击我跳";
        button.setText(str);
        Paint paint = new Paint();
        Rect textBound = new Rect();
        paint.getTextBounds(str, 0, str.length(), textBound);
        int w = textBound.width();
        int h = textBound.height();
        Log.d("lh","===w==="+w+"   ====h="+h);


        startServiceActivity.setText("跳转到serviceactivity");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Bitmap mBmp = BitmapFactory.decodeResource(getResources(), R.drawable.timg);
                Bitmap b1 = Bitmap.createScaledBitmap(mBmp, 2048, 1024, false);
                //intent.putExtra("byte data", b1);

                Toast.makeText(Main2Activity.this, "测试山山水水", Toast.LENGTH_LONG).show();
                    /*Intent intent = new Intent(Main2Activity.this, HomeActivity.class);

                    //intent.putExtra("l",b1);
                    // 模拟Caused by: android.os.TransactionTooLargeException: data parcel size 8389192 bytes
                    startActivity(intent);*/
                Intent intent = new Intent(Main2Activity.this, Main4Activity.class);
                startActivity(intent);


            }
        });

        startServiceActivity.setOnClickListener((l) -> {
                    Intent intent = new Intent(Main2Activity.this, MainActivity.class);
                    startActivity(intent);
                }
        );

        startLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ILoginService loginService = ServiceLoader.load(ILoginService.class, null).get(0);
                if (loginService != null) {
                    loginService.login(Main2Activity.this);
                }

            }
        });
        startMain3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Main2Activity.this, com.example.myapplication.daytwo.Main3Activity.class);
                Main2Activity.this.startActivity(intent);
            }
        });

        //test1();
        //test();
        startMain4.setOnClickListener(view -> {
            Intent intent = new Intent(Main2Activity.this, MainEventActivity.class);
            Main2Activity.this.startActivity(intent);

        });
        addMapTest();

    }

    private void test1() {
        int retryCount = 0;
        try {
            // while (retryCount < 3){
            Class<?> clazz = Class.forName("com.example.myapplication.day3.HomeImpl");
            Constructor<?> declaredConstructor = clazz.getDeclaredConstructor(Object.class, Bitmap.class);
            declaredConstructor.newInstance();

            //Method method = clazz.getDeclaredMethod("getInstance");
            Method methodPrint = clazz.getDeclaredMethod("print", String.class);
            //method.invoke(clazz);
            Object object = clazz.newInstance();
            methodPrint.invoke(object, "刘欢测试");
            Field agentFiled = clazz.getDeclaredField("agent");
            agentFiled.setAccessible(true);

            Log.i("lh", "----循环次数===" + retryCount+"结果 =" +agentFiled.get(object));
               /* if(retryCount == 0){

                }
                return;*/
            //  }
        } catch (Exception e) {
            e.printStackTrace();
            retryCount++;
            //throw new Exception();
            Log.i("lh", "循环次数===" + retryCount);

        }
    }


    public static void test() {
        try {
            Class<?> clazz = Class.forName("com.example.myapplication.day3.People");
            //clazz.getDeclaredAnnotation(String.class);
            Log.i("lh","注解值是---"+clazz.getAnnotation(RequestType.class).value());
            Constructor<?> constructor = clazz.getDeclaredConstructor(int.class,String.class);

            People p = (People) constructor.newInstance(3,"赵四"/*new Object[] { 3 }*/);

            Field declaredField = clazz.getDeclaredField("REPORT");
            declaredField.setAccessible(true);
            p.speak();
            Method method = clazz.getDeclaredMethod("sayHello",String.class);
            String o = (String) method.invoke(p,"测试");

            Log.i("lh","o="+o);
             Log.i("lh","declaredField="+declaredField.get(null));

           /* Method method = clazz.getDeclaredMethod("drink");
            method.invoke(p);*/
           /* Class<?> clazzLoader = clazz.getClassLoader().loadClass("com.example.myapplication.Animal");

            Method eat = clazz.getMethod("eat");
            eat.invoke(p);
            method.setAccessible(true);
            method.invoke(p,"测试");*/

            //ExampleUnitTest instance = (ExampleUnitTest) clazz.newInstance();


        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    public void addMapTest() {
        Observer.create(new OnSubscribe<String>() {
            @Override
            public void call(Subscribe<? super String> subscribe) {
                Log.d("lh", "男生:走，一起去看电影啊!!" + "当前线程为：" + Thread.currentThread().getName());
                subscribe.onNext("男生:走，一起去看电影啊");
            }
        }).map(new Fun1<String, Bitmap>() {
            @Override
            public Bitmap call(String s) {
                Log.d("lh", "闺蜜:走，我好朋友找到我，我可以去开房!!");
                return null;
            }
        }).substrubeIo()
                .observOnMainUI()
                .substrube(new Subscribe<String>() {
                    @Override
                    public void onNext(String s) {
                        Log.d("lh", "男生说的话:" + s + "当前线程为：" + Thread.currentThread().getName());
                        Log.d("lh", "女生:好啊,今天天气不错，走!!");
                    }
                });
    }
}
