````markdown
# 库存扣减方案对比

## 背景

商品库存扣减需要防止超卖。库存初始值 10，200 个线程并发扣减，每个线程循环请求扣减接口。

压测工具：JMeter  
压测条件：200 线程，Loop Count 100，库存初始值 10

---

## 方案一：先查后改（错误示范）

```java
Integer quantity = inventoryMapper.selectQuantity(goodsId);
if (quantity >= num) {
    inventoryMapper.updateQuantity(goodsId, quantity - num);
}
```

**问题**：“查”和“改”是两步操作，中间有空隙。多个线程同时读到同一个旧库存值，都判断“够扣”，都执行 UPDATE，导致超卖。

**实测结果**：
- 库存最终值：0
- 流水表条数：1206
- 结论：1206 次扣减“成功”写入流水，但库存只够扣 10 次，超卖 1196 件

**为什么流水会有 1206 条**：每个线程查到 `quantity = 10` 时都认为够扣，都执行了 UPDATE 和 INSERT 流水。但后续的 UPDATE 覆盖了前面的更新，库存最终只反映了 10 次有效扣减。

---

## 方案二：悲观锁（SELECT ... FOR UPDATE）

```sql
SELECT quantity FROM inventory WHERE goods_id = 1 FOR UPDATE;
UPDATE inventory SET quantity = quantity - 1 WHERE goods_id = 1;
```

**原理**：查询时用 `FOR UPDATE` 锁住这一行，其他事务必须等锁释放才能查询。

**优点**：强一致，不会超卖。

**缺点**：串行执行，并发高时大量请求排队等锁，吞吐量低。锁等待还可能触发超时。

**适用场景**：并发量低、对一致性要求极高的场景。

---

## 方案三：乐观锁（@Version + 重试）

```sql
UPDATE inventory 
SET quantity = quantity - 1, version = version + 1 
WHERE goods_id = 1 AND version = #{oldVersion};
```

**原理**：更新时校验版本号。版本号不匹配说明这行被别人改过，当前更新失败，需要重新查询再重试。

**优点**：冲突少时性能好，不会长时间持锁。

**缺点**：高并发下大量请求因版本冲突失败，需要重试。重试本身又增加数据库压力，形成恶性循环。

**适用场景**：读多写少、冲突概率低的场景。

---

## 方案四：原子 SQL（本项目采用）

```sql
UPDATE inventory 
SET quantity = quantity - #{num} 
WHERE goods_id = #{goodsId} AND quantity >= #{num};
```

**原理**：把判断条件 `quantity >= #{num}` 放进 UPDATE 的 WHERE 里。数据库执行时先锁定这一行，再检查条件，满足才扣减。检查和扣减在同一个数据库操作里完成，中间没有空隙。

**优点**：
- 一次数据库交互，无重试
- 实现简单，代码量少
- 性能优于乐观锁，因为不需要版本号校验和重试
- 靠数据库行锁保证正确性，不依赖应用层逻辑

**缺点**：
- 只适合单行库存扣减
- 如果涉及多张表联动扣减，需要额外设计分布式事务或补偿逻辑

**实测结果**：
- 库存最终值：0
- 流水表条数：10
- 结论：只有前 10 个线程 `affected = 1`，写流水；后面全部 `affected = 0`，不写流水。无超卖。

---

## 四种方案对比总结

| 方案     | 一致性     | 性能             | 实现复杂度 | 适用场景         |
| -------- | ---------- | ---------------- | ---------- | ---------------- |
| 先查后改 | 差，会超卖 | 高               | 低         | 错误示范，不可用 |
| 悲观锁   | 强         | 低，串行等待     | 中         | 并发低、强一致   |
| 乐观锁   | 强         | 中，高并发重试多 | 中         | 读多写少、冲突低 |
| 原子 SQL | 强         | 高，无重试       | 低         | 单行库存扣减     |

---

## 为什么选原子 SQL

1. 不需要版本号重试，高并发下失败率更低。
2. 一次 UPDATE 完成判断和扣减，数据库行锁天然保证正确性。
3. 实现简单，代码量少，容易讲清楚。
4. 在当前项目场景（单行库存、并发量不大）下，收益和复杂度平衡最好。

---

## 问答

**Q：为什么不用乐观锁？**

A：乐观锁在高并发下大量请求会因版本冲突失败，需要重试。重试又增加数据库压力。原子 SQL 用数据库行锁直接保证正确性，不需要重试，高并发下失败率更低。

**Q：为什么不用悲观锁？**

A：悲观锁用 `SELECT ... FOR UPDATE` 锁住行，其他请求排队等待，吞吐量低。原子 SQL 只在 UPDATE 执行时锁行，锁的粒度更小，性能更好。

**Q：为什么不用 Redis 预扣？**

A：Redis 预扣适合秒杀级别的高并发，但引入缓存和数据库的一致性问题，复杂度高。我的场景并发量不大，原子 SQL 够用。如果后续并发量上来，可以考虑 Redis 预扣 + 异步落库。

**Q：原子 SQL 的缺点是什么？**

A：只适合单行库存扣减。如果涉及多张表联动扣减，需要额外设计分布式事务或补偿逻辑。另外，它依赖数据库行锁，如果数据库本身成为瓶颈，需要考虑其他方案。

**Q：流水表的前后值准确吗？**

A：不完全准确。查询当前库存和原子扣减之间仍然有空隙，`before` 值在高并发下可能偏差。但核心防超卖依赖原子 UPDATE，不依赖这个前后值。如果要保证流水前后值精确，需要用 `SELECT ... FOR UPDATE` 把查询也锁住，或者用乐观锁重试。

**Q：怎么验证原子 SQL 真的防住了超卖？**

A：用 JMeter 做对比压测。库存设为 10，200 线程并发。先查后改的方案跑完后流水表写入 1206 条，库存只扣了 10 次，超卖 1196 件。原子扣减方案同样条件下流水表只增加 10 条，库存停在 0，无超卖。

---

## 压测数据记录

| 方案     | 库存初始 | 实际扣减次数 | 流水表条数 | 结论         |
| -------- | -------- | ------------ | ---------- | ------------ |
| 先查后改 | 10       | 10           | 1206       | 超卖 1196 件 |
| 原子扣减 | 10       | 10           | 10         | 无超卖       |

压测条件：JMeter，200 线程，Loop Count 100，库存初始值 10。

---

## 缓存设计

商品详情查询加了 Redis 缓存：

- 缓存 key：`goods:{id}`
- 正常数据过期时间：5 分钟
- 空值过期时间：1 分钟（防穿透）

**防穿透**：查询不存在的商品时，缓存字符串 `"null"`，避免同一个不存在的 id 反复打数据库。

**未处理**：缓存击穿（热点 key 过期）和缓存雪崩（大量 key 同时过期）。当前场景并发低、数据量小，引入互斥锁或随机过期时间收益不明显。如果上线后并发量上来，会优先给热点 key 加互斥锁、给过期时间加随机值。

### 缓存核心代码

```java
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
```

**关键点说明**：
- 空值缓存过期时间设 1 分钟，正常数据设 5 分钟。空值只是为了挡住短时间内的重复查询，不需要存太久。
- 用 `StringRedisTemplate` 而非 `RedisTemplate`，存进去的是可读 JSON，方便用 `redis-cli` 直接查看。
- 没有引入互斥锁或布隆过滤器，因为当前场景并发低、数据量小。
````
