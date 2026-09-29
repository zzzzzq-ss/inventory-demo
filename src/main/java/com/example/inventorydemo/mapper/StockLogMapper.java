package com.example.inventorydemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.inventorydemo.entity.StockLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockLogMapper extends BaseMapper<StockLog> {
}
