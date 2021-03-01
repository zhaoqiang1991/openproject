package com.example.myapplication;

public class People extends Animal{
    private int age;
    private String name;

    public People(int age, String name) {
        this.age = age;
        this.name = name;
        System.out.println("---age----"+age+"name="+name);
    }

    public People(int age) {
        this.age = age;
        System.out.println("---age----"+age);
    }

    public void sayHello(String str){
        System.out.println("---sayhello----"+str);
    }
}
