package com.example.inventorydemo.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StockStrategyFactory {

    // Spring启动时，把所有实现类按bean名称注入到Map里
    @Autowired
    private Map<String, StockDeductStrategy> strategyMap = new ConcurrentHashMap<>();

    public StockDeductStrategy getStrategy(String type) {
        // 根据商品类型匹配，比如"normal"、"seckill"
        if ("seckill".equals(type)) {
            return strategyMap.get("seckillStockStrategy");
        }
        // 默认走普通商品
        return strategyMap.get("normalStockStrategy");
    }
}