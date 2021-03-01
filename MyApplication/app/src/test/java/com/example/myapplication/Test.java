package com.example.myapplication;

import com.example.myapplication.day3.People;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class Test {
    public static void main(String[] args) {
        try {
            Class<?> clazz = Class.forName("com.example.myapplication.day3.People");
            Constructor<?> constructor = clazz.getDeclaredConstructor(int.class);
            com.example.myapplication.day3.People p = (People) constructor.newInstance(5/*new Object[] { 3 }*/);

            Method method = clazz.getDeclaredMethod("sayHello", String.class);

            Class<?> clazzLoader = clazz.getClassLoader().loadClass("com.example.myapplication.Animal");

            Method eat = clazz.getMethod("eat");
            eat.invoke(p);
            method.setAccessible(true);
            method.invoke(p,"测试");

            //ExampleUnitTest instance = (ExampleUnitTest) clazz.newInstance();


        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
