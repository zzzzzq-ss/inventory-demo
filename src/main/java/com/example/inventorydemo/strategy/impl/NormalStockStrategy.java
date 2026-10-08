package com.example.inventorydemo.strategy.impl;

import com.example.inventorydemo.service.InventoryService;
import com.example.inventorydemo.strategy.StockDeductStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("normalStockStrategy")
public class NormalStockStrategy implements StockDeductStrategy {

    @Autowired
    private InventoryService inventoryService;

    @Override
    public boolean deduct(Long goodsId, Integer num) {
        // 调用 Service 的完整扣减逻辑（包含原子SQL + 流水记录）
        return inventoryService.deduct(goodsId, num);
    }
}