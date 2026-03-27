package org.example.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author : hejiale
 * @version : 1.0
 * @date 2025/3/3 15:16
 */
public class ThreadPool {
    public static void main(String[] args) {
        //Executors.newFixedThreadPool();
        testCompletionService();
    }

    /**
     * get()方法会阻塞方法
     * 可以指定索引来决定先获取哪个任务的执行结果
     */
    public void testFuture(){
        ExecutorService executor = Executors.newFixedThreadPool(2);
        // 提交任务并保存 Future 对象
        List<Future<String>> futures = new ArrayList<>();
        futures.add(executor.submit(() -> {
            Thread.sleep(5000); // 模拟耗时任务
            return "Task 1 Result";
        }));

        futures.add(executor.submit(() -> {
            Thread.sleep(1000); // 较快的任务
            return "Task 2 Result";
        }));
        // 不按提交顺序获取结果
        try {
            // 获取第二个任务的结果
            System.out.println("Task 2 Result: " + futures.get(1).get());
            // 获取第一个任务的结果
            System.out.println("Task 1 Result: " + futures.get(0).get());
        } catch (Exception e) {
            e.printStackTrace();
        }

        executor.shutdown();
    }

    /**
     * completionService.take()会按照任务完成的顺序返回结果，
     * 而不是提交的顺序，避免get方法的阻塞
     * 可以在返回结果中添加标识来判断是哪一个任务的结果
     */
    public static void testCompletionService(){
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CompletionService<String> completionService = new ExecutorCompletionService<>(executor);

        // 提交任务
        completionService.submit(() -> {
            Thread.sleep(5000); // 模拟耗时任务
            return "Task 1 Result";
        });

        completionService.submit(() -> {
            Thread.sleep(1000); // 较快的任务
            return "Task 2 Result";
        });

        // 按完成顺序获取结果
        try {
            Future<String> future1 = completionService.take(); // 获取第一个完成的任务
            System.out.println("First completed task: " + future1.get());

            Future<String> future2 = completionService.take(); // 获取第二个完成的任务
            System.out.println("Second completed task: " + future2.get());
        } catch (Exception e) {
            e.printStackTrace();
        }

        executor.shutdown();
    }
}
