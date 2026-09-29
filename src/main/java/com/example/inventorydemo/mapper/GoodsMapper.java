package com.example.inventorydemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.inventorydemo.entity.Goods;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {
}
