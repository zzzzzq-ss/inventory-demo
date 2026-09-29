package com.example.inventorydemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stock_log")
public class StockLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long goodsId;

    private Integer beforeQuantity;

    private Integer afterQuantity;

    private Integer changeNum;

    private LocalDateTime createdAt;
}
