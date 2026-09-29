
# 库存扣减系统

## 项目简介

基于 Spring Boot + MyBatis-Plus + MySQL + Redis 的库存管理模块，重点解决并发扣减场景下的超卖问题，并用 JMeter 做了对比压测验证。

## 技术栈

- Spring Boot 4
- MyBatis-Plus
- MySQL
- Redis（商品详情缓存 + 防穿透）
- JMeter（压测验证）
- Lombok
- 原生 HTML + JavaScript（演示页面）

## 核心功能

1. **库存原子扣减**：把判断条件放进 UPDATE 的 WHERE 里，靠数据库行锁防止超卖
2. **库存流水记录**：每次扣减记录前后值和变化量，可追溯、可对账
3. **商品详情缓存**：Redis 缓存 + 空值防穿透
4. **统一返回体**：所有接口返回 `{code, message, data}` 格式
5. **前端演示页面**：可视化展示商品、库存和流水

## 项目结构

```text
src/main/java/com/example/inventorydemo
├── controller
│   ├── GoodsController          # 商品增删改查
│   ├── InventoryController      # 库存扣减、库存查询
│   └── StockLogController       # 流水查询
├── service
│   ├── GoodsService             # Redis 缓存 + 防穿透
│   └── InventoryService         # 原子扣减 + 流水记录
├── mapper
│   ├── GoodsMapper
│   ├── InventoryMapper
│   └── StockLogMapper
├── entity
│   ├── Goods
│   ├── Inventory
│   └── StockLog
└── common
    └── Result                   # 统一返回体
    
src/main/resources
├── application.properties
└── static
    └── index.html               # 前端演示页面
```
## 接口列表

### 商品

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/goods/list` | 查询所有商品 |
| GET | `/goods/{id}` | 查询单个商品（带 Redis 缓存） |
| POST | `/goods` | 新增商品 |

### 库存

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/inventory/deduct?goodsId=1&num=1` | 原子扣减库存 |
| GET | `/inventory/{goodsId}` | 查询当前库存 |

### 流水

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/stock-log/list?goodsId=1` | 查询某商品的扣减流水 |

### 统一返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```
## 压测对比

压测条件：JMeter，200 线程，Loop Count 100，库存初始值 10。

| 方案     | 库存初始 | 实际扣减次数 | 流水表条数 | 结论         |
| -------- | -------- | ------------ | ---------- | ------------ |
| 先查后改 | 10       | 10           | 1206       | 超卖 1196 件 |
| 原子扣减 | 10       | 10           | 10         | 无超卖       |

**结论**：先查后改的方案在高并发下出现严重超卖，原子扣减方案无超卖，且少了一次数据库交互，性能更好。

## 方案设计

四种库存扣减方案（先查后改、悲观锁、乐观锁、原子 SQL）的详细对比、选型理由和问答，见：

- [docs/stock-deduct-design.md](docs/stock-deduct-design.md)

## 缓存设计

商品详情查询加了 Redis 缓存：

- 缓存 key：`goods:{id}`
- 正常数据过期时间：5 分钟
- 空值过期时间：1 分钟（防穿透）

**防穿透**：查询不存在的商品时，缓存字符串 `"null"`，避免同一个不存在的 id 反复打数据库。

**未处理**：缓存击穿和缓存雪崩。当前场景并发低、数据量小，引入互斥锁或随机过期时间收益不明显。

## 前端演示页面

项目自带一个简单的静态页面，用来演示库存扣减和流水查询。

启动项目后访问：

```
http://localhost:8080/index.html
```

页面功能：

- 展示商品列表和当前库存
- 点击“扣减 1 件”按钮，调用原子扣减接口
- 实时刷新库存和流水记录

前端使用原生 HTML + JavaScript，未引入前端框架，重点在后端并发逻辑。

## 快速开始

1. 创建数据库 `inventory_demo`，执行 `docs/schema.sql`
2. 修改 `src/main/resources/application.properties` 里的数据库和 Redis 配置
3. 确保本机 Redis 已启动
4. 运行 `InventoryDemoApplication`
5. 测试接口：
   - 页面：`http://localhost:8080/index.html`
   - 商品列表：`GET http://localhost:8080/goods/list`
   - 库存扣减：`POST http://localhost:8080/inventory/deduct?goodsId=1&num=1`
   - 流水查询：`GET http://localhost:8080/stock-log/list?goodsId=1`
