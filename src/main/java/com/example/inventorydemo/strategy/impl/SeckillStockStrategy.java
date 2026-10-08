package com.example.inventorydemo.strategy.impl;

import com.example.inventorydemo.strategy.StockDeductStrategy;
import org.springframework.stereotype.Service;

@Service("seckillStockStrategy")
public class SeckillStockStrategy implements StockDeductStrategy {
    @Override
    public boolean deduct(Long goodsId, Integer num) {
        throw new UnsupportedOperationException("秒杀扣减逻辑待实现");
    }
}