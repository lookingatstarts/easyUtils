package com.easyutils;

public class Demo {
    public static void main(String[] args) {
        String line = "1#dd1";
        // 去处#注释
        final int ci = line.indexOf('#');
        if (ci >= 0) {
            line = line.substring(0, ci);
        }
        System.out.println(line);
    }
}
