package com.example.serviceloader;

import android.content.Context;
import android.nfc.Tag;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//应用层介入桥梁
public class ServiceLoader {
    private static final String SERVICE_FILE_NAME = "serviceloader/services";
    private static final int VM_WITH_MULTED_VERSION_MAJOR = 2;
    private static final int VM_WITH_MULTED_VERSION_MINJOR = 1;
    public static final String TAG = ServiceLoader.class.getSimpleName();
    private static Map<String, Map<String, String>> servicesMap;
    private static Context appContext;
    private static volatile boolean loadServices = false;
    private static volatile boolean isNotify = false;
    private static OnErrorListener errorListener;
    private static int initTime = 0;
    private static HandlerThread sHanderThread;
    private static Handler sHandler;
    private static HandlerThread sBgHandlerThread;
    private static Handler sBgHandler;
    private static Object mLock = new Object();

    /**
     * 是否已经初始化完成
     *
     * @return
     */
    public static boolean isInited() {
        return loadServices;
    }

    public static void setAppContext(Context context) {
        if (context != null && context.getApplicationContext() != null) {
            appContext = context.getApplicationContext();
        }
    }


    public static Map<String, Map<String, String>> serviceMap() {
        if (!isInited() && (appContext == null)) {
            init(appContext, errorListener);
        }
        Map<String, Map<String, String>> map = new HashMap<>();
        if (servicesMap != null && !servicesMap.isEmpty()) {
            for (Map.Entry<String, Map<String, String>> entry : servicesMap.entrySet()) {
                Map<String, String> implMap = new HashMap<>();
                if (entry.getValue() != null) {
                    implMap.putAll(entry.getValue());
                }
                map.put(entry.getKey(), implMap);
            }
        }
        return map;
    }


    public static void asyncInit(final Context context, final OnErrorListener listener) {
        appContext = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                init(context, listener);
            }
        }).start();

    }

    public static void getServiceMap() {

    }

    public static synchronized void init(final Context context, OnErrorListener listener) {
        errorListener = listener;
        if (loadServices) {
            return;
        }
        appContext = context.getApplicationContext();
        long curTime = SystemClock.uptimeMillis();
        initTime++;
        if (servicesMap != null && !servicesMap.isEmpty()) {
            servicesMap.clear();
        }
        try {
            //从插入的代码中获取服务声明
            getServiceMap();
        } catch (Exception e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onError(new RuntimeException("serviceloader init error!!!"));
            }
            e.printStackTrace();
        }

        if (servicesMap != null && !servicesMap.isEmpty()) {
            //serviceMap已经初始化完毕
            loadServices = true;
            Log.i(TAG, "initSuccessed 耗时---" + (SystemClock.uptimeMillis() - curTime));
            return;
        }
        long startTime = SystemClock.uptimeMillis();
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(appContext.getAssets().open(SERVICE_FILE_NAME));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] configs = line.split(":");
                if (configs.length != 2 || TextUtils.isEmpty(configs[0]) || TextUtils.isEmpty(configs[1])) {
                    continue;
                }
                //[kye1,service1][key2,service2]....
                String[] declarations = configs[1].split("\\[|\\]\\[|\\]");
                Map<String, String> declarationMap = new HashMap<>(declarations.length);
                for (String declaration : declarations) {
                    if (TextUtils.isEmpty(declaration)) {
                        continue;
                    }
                    String[] map = declaration.split(",");
                    if (map.length != 2 || TextUtils.isEmpty(map[0]) || TextUtils.isEmpty(map[1])) {
                        continue;
                    }
                    declarationMap.put(map[0], map[1]);
                }
                if (!declarationMap.isEmpty()) {
                    if (servicesMap == null) {
                        servicesMap = new HashMap<>();
                    }
                    servicesMap.put(configs[0], declarationMap);
                }
            }
            Log.i(TAG, "serviceloader inti from asset cost time" + (SystemClock.uptimeMillis() - startTime));
            loadServices = true;
        } catch (IOException e) {
            e.printStackTrace();
            loadServices = false;
        }

    }

    /**
     * 根据用户设置的key加载实现类，如果key是空的，那么返回所有的实现类
     *
     * @param service
     * @param key
     * @param params
     * @param <T>
     * @return
     */
    public static <T> List<T> load(final Class<T> service, final String key, Object... params) {
        if (service == null) return Collections.emptyList();

        if (!loadServices) {
            init(appContext, errorListener);
        }
        if (servicesMap == null || serviceMap().isEmpty()) {
            if (errorListener != null) {
                errorListener.onError(new RuntimeException("load" + service.getName() + "," + "key" + key));
            }
            return Collections.emptyList();
        }
        Collection<String> implementers;
        Map<String, String> declaratiomMap = serviceMap().get(service.getName());
        if (declaratiomMap != null && !declaratiomMap.isEmpty()) {
            if (TextUtils.isEmpty(key)) {
                implementers = declaratiomMap.values();
            } else {
                String className = declaratiomMap.get(key);
                implementers = Collections.singleton(className);
            }
            if (implementers != null) {
                ArrayList<T> serviceList = new ArrayList<>(implementers.size());
                ClassLoader classLoader = appContext.getClassLoader();
                for (String className : implementers) {
                    if (!TextUtils.isEmpty(className)) {
                        T implementer = createInstance(classLoader, className, params);
                        if (null == implementer) {
                            Log.i(TAG, "serviceloader load class failed className=" + className);
                        } else {
                            serviceList.add(implementer);
                        }
                    }
                }
                return serviceList;
            }
        }
        return Collections.emptyList();
    }

    /**
     * 等待条件到达之后再去加载
     *
     * @param <T>
     * @return
     */
    public static <T> List<T> loadByCondation(final Class<T> service, final String key, long timeOut, Object... parameter) {
        if (service == null) {
            return Collections.emptyList();
        }

        if (!loadServices) {
            init(appContext, errorListener);
        }
        if (servicesMap == null || serviceMap().isEmpty()) {
            if (errorListener != null) {
                errorListener.onError(new RuntimeException("load" + service.getName() + "," + "key" + key));
            }
            return Collections.emptyList();
        }

        Map<String, String> declaratiomMap = serviceMap().get(service.getName());
        if (declaratiomMap != null && !declaratiomMap.isEmpty()) {
            Collection<String> implementers;
            ArrayList<String> failedClassName = new ArrayList<>();
            if (TextUtils.isEmpty(key)) {
                implementers = declaratiomMap.values();
            } else {
                String className = declaratiomMap.get(key);
                implementers = Collections.singleton(className);
            }
            if (implementers != null) {
                ArrayList<T> serviceList = new ArrayList<>(implementers.size());
                ClassLoader classLoader = appContext.getClassLoader();
                for (String className : implementers) {
                    if (!TextUtils.isEmpty(className)) {
                        T implementer = createInstance(classLoader, className, parameter);
                        if (null == implementer) {
                            //放在失败集合里面
                            failedClassName.add(className);
                            Log.i(TAG, "serviceloader load class failed className=" + className);
                        } else {
                            serviceList.add(implementer);
                        }
                    }
                }
                if (!failedClassName.isEmpty() && !isNotify) {
                    synchronized (mLock) {
                        try {
                            mLock.wait(timeOut);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Log.i(TAG, "serviceloader wait failed!!" + e.getStackTrace());
                        }
                        for (String faileClassName : failedClassName) {
                            T failedImplementer = createInstance(classLoader, faileClassName, parameter);
                            Log.i(TAG, "try to load failed class" + faileClassName + " result = " + (null == failedImplementer));
                            if (failedImplementer != null) {
                                serviceList.add(failedImplementer);
                            }
                        }

                    }
                }

                return serviceList;
            }
        }
        return Collections.emptyList();
    }

    public static synchronized <T> void asyncLoad(final Class<T> service, final String key, final Callback callback,
                                                  final Object parameter) {
        if (Looper.myLooper() == null) {
            throw new RuntimeException("Thread has not looper");
        }
        if (sHanderThread == null) {
            sHanderThread = new HandlerThread("service loader");
            sHanderThread.start();
        }
        if (sHandler == null) {
            sHandler = new Handler(sHanderThread.getLooper());
        }
        if (sHandler != null) {
            final Handler handler = new Handler();
            sHandler.post(new Runnable() {
                @Override
                public void run() {
                    final List<T> objects = load(service, key, parameter);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onLoad(objects);
                        }
                    });

                }
            });
        }
    }

    /**
     * 异步加载，等待条件满足时候才返回
     *
     * @param service
     * @param key
     * @param callback
     * @param parameter
     * @param <T>
     */
    public static synchronized <T> void asyncLoadByCondition(final Class<T> service, final String key, final Callback callback, final
    long timeOut, final Object parameter) {
        if (sBgHandlerThread == null) {
            sBgHandlerThread = new HandlerThread("service loader");
            sBgHandlerThread.start();
        }

        if (sBgHandler == null) {
            sBgHandler = new Handler(sBgHandlerThread.getLooper());
        }

        if (sBgHandler != null) {
            sBgHandler.post(new Runnable() {
                @Override
                public void run() {
                    final List<T> objects = loadByCondation(service, key, timeOut, parameter);
                    Handler handler;
                    if (Looper.myLooper() == null) {
                        handler = new Handler(Looper.getMainLooper());
                    } else {
                        handler = new Handler();
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onLoad(objects);
                        }
                    });
                }
            });
        }
    }

    public static void notify(boolean flag) {
        Log.i(TAG, "serviceloader notify" + flag);
        isNotify = flag;
        if (isNotify) {
            synchronized (mLock) {
                mLock.notifyAll();
            }
        }
    }

    private static <T> T createInstance(ClassLoader classLoader, String className, Object[] params) {
        T implementer = null;
        int length = params.length;
        if (length == 0) {
            /**没有参数
             * 1.动态加载class
             * 2.创建实例对象
             */
            try {//classLoader.loadClass(className).newInstance()失败的话可以采用这种方案classLoader.loadClass(className).getConstructor().newInstance();//
                implementer = (T) classLoader.loadClass(className).newInstance();
            } catch (Throwable e) {
                e.printStackTrace();
                if (errorListener != null) {
                    errorListener.onError(new RuntimeException("classLoader.loadClass(className).newInstance() exception" + e.getStackTrace()));
                }
            }
        } else {
            Class<?>[] classes = new Class<?>[length];
            for (int i = 0; i < length; i++) {
                classes[i] = params[i].getClass();
            }
            Throwable throwable = null;
            try {
                Constructor<?>[] constructors = classLoader.loadClass(className).getConstructors();
                for (Constructor constructor : constructors) {
                    Class[] constructorClasses = constructor.getParameterTypes();
                    if (classes.length == constructorClasses.length) {
                        for (int i = 0; i < classes.length; i++) {
                            //判定此 Class 对象所表示的类或接口与指定的 Class 参数所表示的类或接口是否相同，或是否是其超类或超接口
                            if (constructorClasses[i].isAssignableFrom(classes[i])) {
                                if (i == classes.length - 1) {
                                    Constructor con = classLoader.loadClass(className).getConstructor(constructorClasses);
                                    implementer = (T) con.newInstance(params);
                                }
                            } else {
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (implementer == null) {
                boolean isPrivate = false;
                for (int i = 0; i < length; i++) {
                    Class cla = classes[i];
                    try {
                        Class type = (Class) cla.getField("TYPE").get(null);
                        if (type != null && type.isPrimitive()) {
                            isPrivate = true;
                            classes[i] = type;
                        }
                    } catch (Throwable e) {

                    }
                }
                if (isPrivate) {
                    try {
                        Constructor<?>[] constructors = classLoader.loadClass(className).getConstructors();
                        for (Constructor constructor : constructors) {
                            Class[] constructorClasses = constructor.getParameterTypes();
                            if (classes.length == constructorClasses.length) {
                                for (int i = 0; i < classes.length; i++) {
                                    //判定此 Class 对象所表示的类或接口与指定的 Class 参数所表示的类或接口是否相同，或是否是其超类或超接口
                                    if (constructorClasses[i].isAssignableFrom(classes[i])) {
                                        if (i == classes.length - 1) {
                                            Constructor con = classLoader.loadClass(className).getConstructor(constructorClasses);
                                            implementer = (T) con.newInstance(params);
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
            if (implementer == null) {
                if (errorListener != null) {
                    errorListener.onError(new RuntimeException("implementer == null"));
                }
            }
        }
        //返回实现类
        return implementer;
    }

    public interface OnErrorListener {
        void onError(Exception e);

    }

    public interface Callback<T> {
        void onLoad(List<T> list);
    }

}
