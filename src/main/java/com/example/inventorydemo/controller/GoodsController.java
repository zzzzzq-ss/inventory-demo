package com.example.inventorydemo.controller;

import com.example.inventorydemo.common.Result;
import com.example.inventorydemo.entity.Goods;
import com.example.inventorydemo.mapper.GoodsMapper;
import com.example.inventorydemo.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private GoodsService goodsService;

    @GetMapping("/list")
    public Result<List<Goods>> list() {
        return Result.success(goodsMapper.selectList(null));
    }

    @GetMapping("/{id}")
    public Result<Goods> getById(@PathVariable Long id) {
        return Result.success(goodsService.getById(id));
    }

    @PostMapping
    public Result<Goods> add(@RequestBody Goods goods) {
        goodsMapper.insert(goods);
        return Result.success(goods);
    }
}
