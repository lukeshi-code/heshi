package org.example.thread;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : hejiale
 * @version : 1.0
 * @date 2025/3/19 22:26
 */
public class ReorderingExample {
    private static int a = 0;
    private static int b = 0;

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            a = 1; // 写入a
            b = 1; // 写入b
        });

        Thread t2 = new Thread(() -> {
            while (b == 0); // 忙等待直到b变为1
            System.out.println(a); // 打印a
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
