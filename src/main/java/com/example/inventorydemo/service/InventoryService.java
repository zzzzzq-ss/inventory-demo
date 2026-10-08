package com.example.inventorydemo.service;

import com.example.inventorydemo.entity.StockLog;
import com.example.inventorydemo.mapper.InventoryMapper;
import com.example.inventorydemo.mapper.StockLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private StockLogMapper stockLogMapper;

    /**
     * 查询当前库存
     * @param goodsId 商品 id
     * @return 当前库存量，商品不存在返回 null
     */
    public Integer getQuantity(Long goodsId) {
        return inventoryMapper.selectQuantity(goodsId);
    }

    /**
     * 原子扣减库存
     * @param goodsId 商品 id
     * @param num 扣减数量
     * @return true 扣减成功，false 库存不足
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deduct(Long goodsId, Integer num) {
        // 先查当前库存，仅用于记录流水的前后值
        Integer before = inventoryMapper.selectQuantity(goodsId);
        if (before == null) {
            return false; // 商品不存在
        }

        // 原子扣减：判断和扣减在同一条 SQL 里，靠数据库行锁防超卖
        int affected = inventoryMapper.deductStock(goodsId, num);
        if (affected == 0) {
            return false; // 库存不足，没扣成功
        }

        // 扣减成功，记录流水
        StockLog log = new StockLog();
        log.setGoodsId(goodsId);
        log.setBeforeQuantity(before);
        log.setAfterQuantity(before - num);
        log.setChangeNum(-num);
        stockLogMapper.insert(log);

        return true;
    }

    /**
     * 有并发问题的“先查后改”扣减（故意演示超卖）
     */
    public boolean deductNaive(Long goodsId, Integer num) {
        // 1. 先查
        Integer before = inventoryMapper.selectQuantity(goodsId);
        if (before == null || before < num) {
            return false;
        }

        // 2. 加大休眠，放大并发窗口（关键！）
        try {
            Thread.sleep(500); // 从 10ms 改成 50ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 3. 直接更新为新值（没有原子 WHERE 条件）
        inventoryMapper.updateQuantity(goodsId, before - num);

        // 4. 打印日志，方便观察
        System.out.println("线程 " + Thread.currentThread().getName()
                + " 读到库存=" + before + "，扣减后=" + (before - num));

        // 5. 记录流水
        StockLog log = new StockLog();
        log.setGoodsId(goodsId);
        log.setBeforeQuantity(before);
        log.setAfterQuantity(before - num);
        log.setChangeNum(-num);
        stockLogMapper.insert(log);

        return true;
    }
}