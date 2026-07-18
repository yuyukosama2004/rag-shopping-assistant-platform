package com.biyesheji.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.biyesheji.entity.Stock;
import com.biyesheji.entity.StockLedger;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.constant.StockLua;
import com.biyesheji.order.mapper.StockLedgerMapper;
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
    private final StockLedgerMapper stockLedgerMapper;

    @Override
    @Transactional
    public boolean deduct(String orderNo, Long skuId, Integer quantity) {
        validateOrderNo(orderNo);
        validateQuantity(quantity);
        MutationResult result = reserveDatabase(orderNo, skuId, quantity);
        if (result == MutationResult.INSUFFICIENT) {
            return false;
        }
        if (result == MutationResult.DUPLICATE) {
            return true;
        }
        if (!isSuccess(execute(StockLua.STOCK_DEDUCT, skuId, quantity))) {
            throw new BizException("库存缓存预留失败");
        }
        return true;
    }

    @Override
    @Transactional
    public void restore(String orderNo, Long skuId, Integer quantity) {
        validateOrderNo(orderNo);
        validateQuantity(quantity);
        if (releaseDatabase(orderNo, skuId, quantity) == MutationResult.DUPLICATE) {
            return;
        }
        if (!isSuccess(execute(StockLua.STOCK_RESTORE, skuId, quantity))) {
            throw new BizException("库存缓存恢复失败");
        }
    }

    @Override
    @Transactional
    public void confirmDeduct(String orderNo, Long skuId, Integer quantity) {
        validateOrderNo(orderNo);
        validateQuantity(quantity);
        if (confirmDatabase(orderNo, skuId, quantity) == MutationResult.DUPLICATE) {
            return;
        }
        if (!isSuccess(execute(StockLua.STOCK_CONFIRM, skuId, quantity))) {
            throw new BizException("库存缓存确认失败");
        }
    }

    @Override
    @PostConstruct
    public void initRedisStock() {
        List<Stock> stocks = stockMapper.selectList(null);
        for (Stock stock : stocks) {
            if (stock.getSkuId() == null) {
                continue;
            }
            String key = stockKey(stock.getSkuId());
            redisUtil.delete(key);
            redisUtil.hSet(key, "total", stock.getTotal().toString());
            redisUtil.hSet(key, "locked", stock.getLocked().toString());
            redisUtil.hSet(key, "available", stock.getAvailable().toString());
        }
        log.info("已从MySQL重建Redis库存，共{}条", stocks.size());
    }

    private MutationResult reserveDatabase(String orderNo, Long skuId, int quantity) {
        Stock stock = lockStock(skuId);
        String eventKey = eventKey("ORDER_RESERVE", orderNo, skuId);
        if (isProcessed(eventKey)) {
            return MutationResult.DUPLICATE;
        }
        if (stock == null || stock.getAvailable() < quantity) {
            return MutationResult.INSUFFICIENT;
        }
        Stock update = snapshot(
                stock.getTotal(),
                stock.getLocked() + quantity,
                stock.getAvailable() - quantity,
                stock.getVersion() + 1);
        updateStock(stock, update, quantity, Stock::getAvailable);
        insertLedger(eventKey, orderNo, "ORDER_RESERVE", quantity, stock, update);
        return MutationResult.APPLIED;
    }

    private MutationResult releaseDatabase(String orderNo, Long skuId, int quantity) {
        Stock stock = lockStock(skuId);
        String eventKey = eventKey("ORDER_RELEASE", orderNo, skuId);
        if (isProcessed(eventKey)) {
            return MutationResult.DUPLICATE;
        }
        if (stock == null || stock.getLocked() < quantity) {
            throw new BizException("库存预留不存在或已处理");
        }
        Stock update = snapshot(
                stock.getTotal(),
                stock.getLocked() - quantity,
                stock.getAvailable() + quantity,
                stock.getVersion() + 1);
        updateStock(stock, update, quantity, Stock::getLocked);
        insertLedger(eventKey, orderNo, "ORDER_RELEASE", quantity, stock, update);
        return MutationResult.APPLIED;
    }

    private MutationResult confirmDatabase(String orderNo, Long skuId, int quantity) {
        Stock stock = lockStock(skuId);
        String eventKey = eventKey("ORDER_CONFIRM", orderNo, skuId);
        if (isProcessed(eventKey)) {
            return MutationResult.DUPLICATE;
        }
        if (stock == null || stock.getLocked() < quantity || stock.getTotal() < quantity) {
            throw new BizException("库存预留不存在或已处理");
        }
        Stock update = snapshot(
                stock.getTotal() - quantity,
                stock.getLocked() - quantity,
                stock.getAvailable(),
                stock.getVersion() + 1);
        updateStock(stock, update, quantity, Stock::getLocked);
        insertLedger(eventKey, orderNo, "ORDER_CONFIRM", quantity, stock, update);
        return MutationResult.APPLIED;
    }

    private Stock lockStock(Long skuId) {
        return stockMapper.selectBySkuIdForUpdate(skuId);
    }

    private boolean isProcessed(String eventKey) {
        return stockLedgerMapper.selectCount(new LambdaQueryWrapper<StockLedger>()
                .eq(StockLedger::getEventKey, eventKey)) > 0;
    }

    private Stock snapshot(int total, int locked, int available, int version) {
        Stock stock = new Stock();
        stock.setTotal(total);
        stock.setLocked(locked);
        stock.setAvailable(available);
        stock.setVersion(version);
        return stock;
    }

    private void updateStock(Stock before, Stock after, int quantity,
                             SFunction<Stock, Integer> guardedField) {
        int rows = stockMapper.update(after, new LambdaUpdateWrapper<Stock>()
                .eq(Stock::getSkuId, before.getSkuId())
                .eq(Stock::getVersion, before.getVersion())
                .ge(guardedField, quantity));
        if (rows != 1) {
            throw new BizException("库存更新发生并发冲突");
        }
    }

    private void insertLedger(String eventKey, String orderNo, String action, int quantity,
                              Stock before, Stock after) {
        StockLedger ledger = new StockLedger();
        ledger.setSkuId(before.getSkuId());
        ledger.setAction(action);
        ledger.setEventKey(eventKey);
        ledger.setQuantity(quantity);
        ledger.setBeforeTotal(before.getTotal());
        ledger.setAfterTotal(after.getTotal());
        ledger.setBeforeLocked(before.getLocked());
        ledger.setAfterLocked(after.getLocked());
        ledger.setBeforeAvailable(before.getAvailable());
        ledger.setAfterAvailable(after.getAvailable());
        ledger.setReferenceNo(orderNo);
        stockLedgerMapper.insert(ledger);
    }

    private String eventKey(String action, String orderNo, Long skuId) {
        return action + ":" + orderNo + ":" + skuId;
    }

    private Long execute(String script, Long skuId, int quantity) {
        return redisUtil.executeLua(script, Collections.singletonList(stockKey(skuId)), quantity);
    }

    private boolean isSuccess(Long result) {
        return result != null && result == 1;
    }

    private String stockKey(Long skuId) {
        return "stock:sku:" + skuId;
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BizException("商品数量必须大于0");
        }
    }

    private void validateOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank() || orderNo.length() > 64) {
            throw new BizException("订单号不能为空且长度不能超过64个字符");
        }
    }

    private enum MutationResult {
        APPLIED,
        DUPLICATE,
        INSUFFICIENT
    }
}
