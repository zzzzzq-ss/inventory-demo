package com.example.inventorydemo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;

@SpringBootTest
class OversellTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void oversellTest() throws Exception {
        // 1. 重置库存为 1
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE inventory SET quantity = 1 WHERE goods_id = 1")) {
            ps.executeUpdate();
        }

        CountDownLatch ready = new CountDownLatch(1);

        Runnable task = () -> {
            try {
                ready.await(); // 等待发令枪，两个线程同时开始

                // 2. 先读取当前库存
                int quantity = 0;
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT quantity FROM inventory WHERE goods_id = 1");
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        quantity = rs.getInt(1);
                    }
                }
                System.out.println(Thread.currentThread().getName() + " 读到库存: " + quantity);

                // 3. 模拟业务耗时，制造并发窗口
                Thread.sleep(200);

                // 4. 执行扣减
                deduct();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        Thread t1 = new Thread(task, "thread-1");
        Thread t2 = new Thread(task, "thread-2");
        t1.start();
        t2.start();

        ready.countDown(); // 同时放行两个线程
        t1.join();
        t2.join();

        // 5. 打印最终库存
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT quantity FROM inventory WHERE goods_id = 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                System.out.println("最终库存: " + rs.getInt(1));
            }
        }
    }

    private void deduct() throws Exception {
        // 版本 A（无防护，会超卖）：
        //   UPDATE inventory SET quantity = quantity - 1 WHERE goods_id = 1
        //
        // 版本 B（加 AND quantity >= 1，原子防超卖）：
        //   UPDATE inventory SET quantity = quantity - 1 WHERE goods_id = 1 AND quantity >= 1
        //
        String sql = "UPDATE inventory SET quantity = quantity - 1 WHERE goods_id = 1 AND quantity >= 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int affected = ps.executeUpdate();
            System.out.println(Thread.currentThread().getName() + " 扣减影响行数: " + affected);
        }
    }
}
