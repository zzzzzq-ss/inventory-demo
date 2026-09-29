package com.example.inventorydemo.service;

import com.example.inventorydemo.entity.Goods;
import com.example.inventorydemo.mapper.GoodsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public Goods getById(Long id) {
        String cacheKey = "goods:" + id;

        // 1. 先查缓存
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            // 空值缓存，说明数据库里没有这个商品
            if ("null".equals(cached)) {
                return null;
            }
            try {
                return objectMapper.readValue(cached, Goods.class);
            } catch (Exception e) {
                throw new RuntimeException("缓存反序列化失败", e);
            }
        }

        // 2. 缓存未命中，查数据库
        Goods goods = goodsMapper.selectById(id);

        // 3. 数据库也没有，缓存空值防穿透
        if (goods == null) {
            stringRedisTemplate.opsForValue().set(cacheKey, "null", 1, TimeUnit.MINUTES);
            return null;
        }

        // 4. 数据库有，写回缓存
        try {
            String json = objectMapper.writeValueAsString(goods);
            stringRedisTemplate.opsForValue().set(cacheKey, json, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("缓存序列化失败", e);
        }
        return goods;
    }
}