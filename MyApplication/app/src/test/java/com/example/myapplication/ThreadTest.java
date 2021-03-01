package com.example.myapplication;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;
import javassist.NotFoundException;

public class ThreadTest {
    public static void main(String[] args) {
        /*BlockingQueue queue = new LinkedBlockingQueue();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 6, 2, TimeUnit.SECONDS, queue);
        for ( int i = 0; i < 20; i++) {
            executor.execute( new Runnable() {
                public void run() {
                    try {
                        System. out.println( this.hashCode()/1000);
                        for ( int j = 0; j < 10; j++) {
                            System. out.println( this.hashCode() + ":" + j);
                            Thread.sleep(this.hashCode()%2);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System. out.println(String. format("thread %d finished", this.hashCode()));
                }
            });
        }*/
        try {
            CtClass ctClass = ClassPool.getDefault().get("com.example.myapplication.Person");
            CtField nameField = ctClass.getField("name");
            nameField.setModifiers(Modifier.PUBLIC | Modifier.VOLATILE);
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
    }
}
