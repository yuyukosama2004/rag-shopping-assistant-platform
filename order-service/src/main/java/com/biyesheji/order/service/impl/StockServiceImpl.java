package com.biyesheji.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.biyesheji.entity.Stock;
import com.biyesheji.exception.BizException;
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

    private static final int MAX_RETRIES = 3;

    private final RedisUtil redisUtil;
    private final StockMapper stockMapper;

    @Override
    @Transactional
    public boolean deduct(Long productId, Integer quantity) {
        validateQuantity(quantity);
        if (!reserveDatabase(productId, quantity)) {
            return false;
        }

        Long result = execute(StockLua.STOCK_DEDUCT, productId, quantity);
        if (result != null && result == 1) {
            return true;
        }

        releaseDatabase(productId, quantity);
        return false;
    }

    @Override
    @Transactional
    public void restore(Long productId, Integer quantity) {
        validateQuantity(quantity);
        releaseDatabase(productId, quantity);
        if (!isSuccess(execute(StockLua.STOCK_RESTORE, productId, quantity))) {
            throw new BizException("库存缓存恢复失败");
        }
    }

    @Override
    @Transactional
    public void confirmDeduct(Long productId, Integer quantity) {
        validateQuantity(quantity);
        confirmDatabase(productId, quantity);
        if (!isSuccess(execute(StockLua.STOCK_CONFIRM, productId, quantity))) {
            throw new BizException("库存缓存确认失败");
        }
    }

    @Override
    @PostConstruct
    public void initRedisStock() {
        List<Stock> stocks = stockMapper.selectList(null);
        for (Stock stock : stocks) {
            String key = stockKey(stock.getProductId());
            redisUtil.delete(key);
            redisUtil.hSet(key, "total", stock.getTotal().toString());
            redisUtil.hSet(key, "locked", stock.getLocked().toString());
            redisUtil.hSet(key, "available", stock.getAvailable().toString());
        }
        log.info("已从MySQL重建Redis库存，共{}条", stocks.size());
    }

    private boolean reserveDatabase(Long productId, int quantity) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Stock stock = getStock(productId);
            if (stock == null || stock.getAvailable() < quantity) {
                return false;
            }
            int oldVersion = stock.getVersion();
            Stock update = new Stock();
            update.setLocked(stock.getLocked() + quantity);
            update.setAvailable(stock.getAvailable() - quantity);
            update.setVersion(oldVersion + 1);
            int rows = stockMapper.update(update, new LambdaUpdateWrapper<Stock>()
                    .eq(Stock::getProductId, productId)
                    .eq(Stock::getVersion, oldVersion)
                    .ge(Stock::getAvailable, quantity));
            if (rows == 1) {
                return true;
            }
        }
        return false;
    }

    private void releaseDatabase(Long productId, int quantity) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Stock stock = getStock(productId);
            if (stock == null || stock.getLocked() < quantity) {
                throw new BizException("库存预留不存在或已处理");
            }
            int oldVersion = stock.getVersion();
            Stock update = new Stock();
            update.setLocked(stock.getLocked() - quantity);
            update.setAvailable(stock.getAvailable() + quantity);
            update.setVersion(oldVersion + 1);
            int rows = stockMapper.update(update, new LambdaUpdateWrapper<Stock>()
                    .eq(Stock::getProductId, productId)
                    .eq(Stock::getVersion, oldVersion)
                    .ge(Stock::getLocked, quantity));
            if (rows == 1) {
                return;
            }
        }
        throw new BizException("库存恢复发生并发冲突");
    }

    private void confirmDatabase(Long productId, int quantity) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Stock stock = getStock(productId);
            if (stock == null || stock.getLocked() < quantity || stock.getTotal() < quantity) {
                throw new BizException("库存预留不存在或已处理");
            }
            int oldVersion = stock.getVersion();
            Stock update = new Stock();
            update.setTotal(stock.getTotal() - quantity);
            update.setLocked(stock.getLocked() - quantity);
            update.setAvailable(stock.getAvailable());
            update.setVersion(oldVersion + 1);
            int rows = stockMapper.update(update, new LambdaUpdateWrapper<Stock>()
                    .eq(Stock::getProductId, productId)
                    .eq(Stock::getVersion, oldVersion)
                    .ge(Stock::getTotal, quantity)
                    .ge(Stock::getLocked, quantity));
            if (rows == 1) {
                return;
            }
        }
        throw new BizException("库存确认发生并发冲突");
    }

    private Stock getStock(Long productId) {
        return stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getProductId, productId));
    }

    private Long execute(String script, Long productId, int quantity) {
        return redisUtil.executeLua(script, Collections.singletonList(stockKey(productId)), quantity);
    }

    private boolean isSuccess(Long result) {
        return result != null && result == 1;
    }

    private String stockKey(Long productId) {
        return "stock:product:" + productId;
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BizException("商品数量必须大于0");
        }
    }
}
