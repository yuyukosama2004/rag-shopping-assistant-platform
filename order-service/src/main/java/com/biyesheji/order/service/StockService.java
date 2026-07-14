package com.biyesheji.order.service;

public interface StockService {
    /**
     * Redis Lua 原子预扣库存
     * @return 是否成功
     */
    boolean deduct(Long skuId, Integer quantity);

    /**
     * 恢复锁定库存（取消/超时）
     */
    void restore(Long skuId, Integer quantity);

    /**
     * 实际物理扣减库存（MQ消费端调用）
     */
    void confirmDeduct(Long skuId, Integer quantity);

    /**
     * 初始化商品库存到 Redis（服务启动时调用）
     */
    void initRedisStock();
}
