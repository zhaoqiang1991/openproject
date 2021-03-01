package com.example.myapplication.day1;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Test {

    public static void main(String[] args) {
       /* Map<String,Integer> map = new HashMap<>();
        map.put("1",22);
        map.put("2",12);
        map.put("3",23);
        map.put("4",22);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        for (Iterator<Map.Entry<String, Integer>> iterator = entries.iterator(); iterator.hasNext();){
            Map.Entry<String, Integer> next = iterator.next();
            System.out.println(next.getKey()+"=="+next.getValue());
        }*/


    }

    public static void study(double money, Consumer<Double> con){
        con.accept(money);
    }
}
