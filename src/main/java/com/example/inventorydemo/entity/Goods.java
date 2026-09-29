package com.example.inventorydemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("goods")
public class Goods {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private BigDecimal price;
}
