package com.example.inventorydemo.controller;

import com.example.inventorydemo.common.Result;
import com.example.inventorydemo.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/inventory/{goodsId}")
    public Result<Integer> getQuantity(@PathVariable Long goodsId) {
        return Result.success(inventoryService.getQuantity(goodsId));
    }

    @PostMapping("/inventory/deduct")
    public Result<String> deduct(@RequestParam Long goodsId, @RequestParam Integer num) {
        boolean success = inventoryService.deduct(goodsId, num);
        return success ? Result.success("OK") : Result.error(400, "库存不足");
    }

    @PostMapping("/inventory/deduct-naive")
    public String deductNaive(@RequestParam Long goodsId, @RequestParam Integer num) {
        boolean success = inventoryService.deductNaive(goodsId, num);
        return success ? "OK" : "FAIL";
    }
}
