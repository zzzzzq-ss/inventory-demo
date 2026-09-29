package com.example.inventorydemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.inventorydemo.common.Result;
import com.example.inventorydemo.entity.StockLog;
import com.example.inventorydemo.mapper.StockLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stock-log")
public class StockLogController {

    @Autowired
    private StockLogMapper stockLogMapper;

    @GetMapping("/list")
    public Result<List<StockLog>> list(@RequestParam Long goodsId) {
        List<StockLog> logs = stockLogMapper.selectList(
                new LambdaQueryWrapper<StockLog>()
                        .eq(StockLog::getGoodsId, goodsId)
                        .orderByDesc(StockLog::getCreatedAt));
        return Result.success(logs);
    }
}
