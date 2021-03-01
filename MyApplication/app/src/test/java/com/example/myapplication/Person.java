package com.example.myapplication;

public class Person {
    private String name;
    private int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
        System.out.println("name:" + this.name + ", age:" + this.age);
    }

    public String show() {
        String msg = "show name:" + this.name + ", age:" + this.age;
        System.out.println(msg);
        return msg;
    }

    public int longRun(long sleep) throws InterruptedException {
        Thread.sleep(sleep);
        return 0;
    }

}