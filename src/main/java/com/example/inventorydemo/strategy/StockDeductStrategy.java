package com.example.inventorydemo.strategy;

public interface StockDeductStrategy {
    boolean deduct(Long goodsId, Integer num);
}