package com.example.myapplication;

import android.util.Log;

import com.example.myapplication.day1.People;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        // assertEquals(4, 2 + 2);
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> result = new HashMap<>();
        Map<String, List<String>> listMap = new HashMap<>();
        List<String> list = new ArrayList<>();
        list.add("11");
        list.add("12");
        list.add("13");
        list.add("14");
        result.put("A", "1");
        result.put("B", "2");
        result.put("C", "3");
        result.put("D", "4");
        result.put("E", "5");


        map.put("A", result);
        listMap.put("list", list);

        for (Map.Entry<String, Map<String, String>> e : map.entrySet()) {
            e.getValue();
        }

    }

    @Test
    public void addd() {
        People p1 = new People();
        People p2 = new People();

        change(p1);
        System.out.println("---------" + p1.hashCode());

        Class<?> p11 = p1.getClass();
        Class<?> p22 = p2.getClass();
        System.out.println("---------" + (p1.getClass() == p2.getClass()));
        System.out.println("---------" + (p11 == p22));
        System.out.println("---------" + (p1 == p2));
        System.out.println("---------" + (p11.toString()) + "--" + p22.toString());
    }

    public void change(People p) {
        System.out.println("---------change before" + p.hashCode());
        People p2 = new People();
        System.out.println("---------change" + p2.hashCode());

    }

    @Test
    public void test2() throws ExecutionException, InterruptedException, TimeoutException {
        final int[] i = {0};
        ForkJoinTask<Integer> future = (ForkJoinTask<Integer>) new ForkJoinPool().submit(new Runnable() {
            @Override
            public void run() {
                i[0] += 3;
            }
        });

        System.out.println(future == null ? "---空" : future.get(300, TimeUnit.MILLISECONDS));
    }

    @Test
    public void test4() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        int[] array = new int[]{2, 1, 8, 4};
        /* copr(array);
         for(int i : array){
             System.out.println("--------"+i);
         }*/
        Class<?> aClass = getClass();
        System.out.println("---"+aClass.getName()+"--"+aClass.getSimpleName());
        Class<?> aClass1 = Class.forName("com.example.myapplication.ExampleUnitTest");
        ExampleUnitTest instance = (ExampleUnitTest) aClass1.newInstance();
        //
        // System.out.println("reselut=" + getValue());
    }

    public int getValue() {
        //todo 研究原理
        int i = 0;
        try {
            return i;
        } catch (Exception e) {

        } finally {
            System.out.println("----" + i);
            i++;
            // return i;
        }

        return i;
    }

    public void copr(int[] arry) {
        for (int i = 0; i < arry.length; i++) {
            for (int j = 0; j < arry.length - i - 1; j++) {
                int temp = 0;
                if (arry[j] > arry[j + 1]) {
                    temp = arry[j];
                    arry[j] = arry[j + 1];
                    arry[j + 1] = temp;
                }
            }
        }
    }


}