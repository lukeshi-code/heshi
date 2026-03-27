package org.example.thread;

import java.util.concurrent.TimeUnit;

/**
 * @author : hejiale
 * @version : 1.0
 * @date 2025/7/9 23:10
 */

public class Runner implements Runnable{
    private long i;
    private volatile boolean running =true;
    @Override
    public void run() {
        System.out.println("current Thread Name:"+Thread.currentThread().getName());
        while (running ){
            i++;
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        System.out.println("Count i= "+i);
        System.out.println("current Thread Name:"+Thread.currentThread().getName());

    }
    public void cancel(){
        running =false;
        System.out.println("running=false");
    }
}