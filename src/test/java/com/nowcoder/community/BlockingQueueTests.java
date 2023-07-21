package com.nowcoder.community;

import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

//@SpringBootTest
public class BlockingQueueTests {

//    @Test
public static void main(String[] args) {
    BlockingQueue queue = new ArrayBlockingQueue(10);
    new Thread(new Produce(queue)).start();
    new Thread(new Consumer(queue)).start();
    new Thread(new Consumer(queue)).start();
    new Thread(new Consumer(queue)).start();
}


}

@AllArgsConstructor
class Produce implements Runnable{

    private BlockingQueue<Integer> queue;


    @Override
    public void run() {
        try {
            for (int i = 0; i < 100; i++) {
                Thread.sleep(20);
                queue.put(i);
                System.out.println(Thread.currentThread().getName() + "生产:" + queue.size());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
@AllArgsConstructor
class Consumer implements Runnable{

    private BlockingQueue<Integer> queue;
    @Override
    public void run() {
        try {
            while (true){
                Thread.sleep(new Random().nextInt(1000));
                queue.take();
                System.out.println(Thread.currentThread().getName() + "消费:" + queue.size());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}