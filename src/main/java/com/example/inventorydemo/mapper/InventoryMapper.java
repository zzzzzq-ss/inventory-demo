package com.example.inventorydemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.inventorydemo.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    @Select("SELECT quantity FROM inventory WHERE goods_id = #{goodsId}")
    Integer selectQuantity(@Param("goodsId") Long goodsId);

    @Update("UPDATE inventory SET quantity = quantity - #{num} " +
            "WHERE goods_id = #{goodsId} AND quantity >= #{num}")
    int deductStock(@Param("goodsId") Long goodsId, @Param("num") Integer num);

    @Update("UPDATE inventory SET quantity = #{quantity} WHERE goods_id = #{goodsId}")
    int updateQuantity(@Param("goodsId") Long goodsId, @Param("quantity") Integer quantity);
}