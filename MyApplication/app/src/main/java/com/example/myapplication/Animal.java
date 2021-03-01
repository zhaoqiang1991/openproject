package com.example.myapplication;

import android.util.Log;

public class Animal {
    private int age;
    private String name;
    protected String address;

    public Animal(int age, String name, String address) {
        this.age = age;
        this.name = name;
        this.address = address;
    }

    public void speak(){
        Log.i("lh","---speak"+address);
    }
}
