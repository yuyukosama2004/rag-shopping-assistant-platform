package com.biyesheji.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.biyesheji.entity.Stock;
import com.biyesheji.order.constant.StockLua;
import com.biyesheji.order.mapper.StockMapper;
import com.biyesheji.order.service.StockService;
import com.biyesheji.utils.RedisUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final RedisUtil redisUtil;
    private final StockMapper stockMapper;

    @Override
    public boolean deduct(Long productId, Integer quantity) {
        String key = "stock:product:" + productId;
        Long result = redisUtil.executeLua(
                StockLua.STOCK_DEDUCT,
                Collections.singletonList(key),
                quantity
        );
        return result != null && result == 1;
    }

    @Override
    @Transactional
    public void restore(Long productId, Integer quantity) {
        // 1. 恢复 Redis 库存
        String key = "stock:product:" + productId;
        redisUtil.executeLua(
                StockLua.STOCK_RESTORE,
                Collections.singletonList(key),
                quantity
        );
        // 2. 同步更新 MySQL locked 字段，保持 Redis 与 MySQL 一致
        Stock stock = stockMapper.selectOne(
                new LambdaQueryWrapper<Stock>().eq(Stock::getProductId, productId)
        );
        if (stock != null && stock.getLocked() >= quantity) {
            stock.setLocked(stock.getLocked() - quantity);
            stock.setAvailable(stock.getAvailable() + quantity);
            stockMapper.updateById(stock);
        }
    }

    @Override
    @Transactional
    public void confirmDeduct(Long productId, Integer quantity) {
        Stock stock = stockMapper.selectOne(
                new LambdaQueryWrapper<Stock>().eq(Stock::getProductId, productId)
        );
        if (stock != null) {
            // 使用乐观锁 version 字段防止并发覆盖
            int oldVersion = stock.getVersion();
            stock.setTotal(stock.getTotal() - quantity);
            stock.setLocked(stock.getLocked() - quantity);
            stock.setAvailable(stock.getTotal() - stock.getLocked());
            stock.setVersion(oldVersion + 1);

            LambdaUpdateWrapper<Stock> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Stock::getProductId, productId)
                   .eq(Stock::getVersion, oldVersion);
            int rows = stockMapper.update(stock, wrapper);
            if (rows == 0) {
                log.warn("confirmDeduct 乐观锁冲突, productId={}, version={}", productId, oldVersion);
            }
        }
    }

    @Override
    @PostConstruct
    public void initRedisStock() {
        log.info("初始化 Redis 库存缓存...");
        List<Stock> stocks = stockMapper.selectList(null);
        for (Stock stock : stocks) {
            String key = "stock:product:" + stock.getProductId();
            // 仅在 Redis 中不存在该商品的库存数据时才写入，防止重启覆盖正在进行的预扣数据
            if (redisUtil.exists(key)) {
                log.info("跳过已存在的 Redis 库存: {}", key);
                continue;
            }
            redisUtil.hSet(key, "total", stock.getTotal().toString());
            redisUtil.hSet(key, "locked", stock.getLocked().toString());
            redisUtil.hSet(key, "available", stock.getAvailable().toString());
        }
        log.info("Redis 库存缓存初始化完成，共 {} 条", stocks.size());
    }
}
