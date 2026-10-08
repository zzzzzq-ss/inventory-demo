package com.example.inventorydemo;

import com.example.inventorydemo.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Test
    void testCreateOrder() {
        // 假设商品 id = 1，扣减 1 件，类型是普通商品
        boolean result = orderService.createOrder(1L, 1, "normal");
        System.out.println("扣减结果: " + result);
    }
}