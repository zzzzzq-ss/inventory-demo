package com.example.inventorydemo.service;

import com.example.inventorydemo.strategy.StockDeductStrategy;
import com.example.inventorydemo.strategy.StockStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    @Autowired
    private StockStrategyFactory strategyFactory;

    /**
     * 下单扣减库存（对外统一入口）
     * @param goodsId   商品 id
     * @param num       扣减数量
     * @param goodsType 商品类型：normal / seckill
     */
    public boolean createOrder(Long goodsId, Integer num, String goodsType) {
        StockDeductStrategy strategy = strategyFactory.getStrategy(goodsType);
        return strategy.deduct(goodsId, num);
    }
}