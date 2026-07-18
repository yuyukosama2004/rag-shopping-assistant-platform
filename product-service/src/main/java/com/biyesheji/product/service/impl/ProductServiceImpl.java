package com.biyesheji.product.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.entity.Product;
import com.biyesheji.dto.MerchantProductSaveDTO;
import com.biyesheji.dto.MerchantSkuSaveDTO;
import com.biyesheji.dto.MerchantSkuUpdateDTO;
import com.biyesheji.dto.StockAdjustDTO;
import com.biyesheji.entity.ProductSku;
import com.biyesheji.entity.Stock;
import com.biyesheji.entity.StockLedger;
import com.biyesheji.exception.BizException;
import com.biyesheji.product.mapper.ProductMapper;
import com.biyesheji.product.mapper.ProductSkuMapper;
import com.biyesheji.product.mapper.StockMapper;
import com.biyesheji.product.mapper.StockLedgerMapper;
import com.biyesheji.product.service.ProductService;
import com.biyesheji.product.service.AiIndexTaskPublisher;
import com.biyesheji.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final RedisUtil redisUtil;
    private final ProductSkuMapper productSkuMapper;
    private final StockMapper stockMapper;
    private final StockLedgerMapper stockLedgerMapper;
    private final AiIndexTaskPublisher aiIndexTaskPublisher;

    private static final String CACHE_PRODUCT = "product:detail:";
    private static final String CACHE_HOT = "product:hot";
    private static final String CACHE_FILTERS = "product:filters";
    private static final String NULL_PLACEHOLDER = "__NULL__";

    @Override
    public Page<Product> page(int pageNum, int pageSize, String brand, String category,
                              BigDecimal minPrice, BigDecimal maxPrice, String keyword, String sort) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);

        if (brand != null && !brand.isEmpty()) {
            wrapper.eq(Product::getBrand, brand);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Product::getCategory, category);
        }
        if (minPrice != null) {
            wrapper.ge(Product::getPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(Product::getPrice, maxPrice);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Product::getName, keyword)
                    .or().like(Product::getBrand, keyword)
                    .or().like(Product::getDescription, keyword));
        }

        // 排序
        if ("price_asc".equals(sort)) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(sort)) {
            wrapper.orderByDesc(Product::getPrice);
        } else if ("sales".equals(sort)) {
            wrapper.orderByDesc(Product::getSales);
        } else {
            wrapper.orderByDesc(Product::getSales);
        }

        return productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Product getById(Long id) {
        String cacheKey = CACHE_PRODUCT + id;
        Product cached = redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 空值防穿透：检查是否缓存了空值标记
        if (NULL_PLACEHOLDER.equals(redisUtil.get(cacheKey + ":null"))) {
            throw new BizException(404, "商品不存在或已下架");
        }

        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == 0) {
            // 缓存空值标记 5 分钟，防止缓存穿透
            redisUtil.set(cacheKey + ":null", NULL_PLACEHOLDER, 5, TimeUnit.MINUTES);
            throw new BizException(404, "商品不存在或已下架");
        }

        redisUtil.set(cacheKey, product, 30, TimeUnit.MINUTES);
        return product;
    }

    @Override
    public List<Product> listByBrand(String brand) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getBrand, brand)
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getSales);
        return productMapper.selectList(wrapper);
    }

    @Override
    public List<Product> hot(int limit) {
        // 先查缓存
        String cacheKey = CACHE_HOT + ":" + limit;
        String cached = redisUtil.get(cacheKey);
        if (cached != null) {
            return JSONUtil.toList(cached, Product.class);
        }

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)
                .orderByDesc(Product::getSales)
                .last("LIMIT " + limit);

        List<Product> products = productMapper.selectList(wrapper);

        // 缓存 10 分钟
        redisUtil.set(cacheKey, JSONUtil.toJsonStr(products), 10, TimeUnit.MINUTES);
        return products;
    }

    @Override
    public Map<String, List<String>> getFilters() {
        Map<String, List<String>> cached = redisUtil.get(CACHE_FILTERS);
        if (cached != null) {
            return cached;
        }

        Map<String, List<String>> filters = new HashMap<>();

        // 使用 SQL DISTINCT 避免全量加载数据到内存
        LambdaQueryWrapper<Product> brandWrapper = new LambdaQueryWrapper<>();
        brandWrapper.select(Product::getBrand).eq(Product::getStatus, 1)
                .groupBy(Product::getBrand);
        List<String> brands = productMapper.selectList(brandWrapper).stream()
                .map(Product::getBrand).distinct().collect(Collectors.toList());

        LambdaQueryWrapper<Product> catWrapper = new LambdaQueryWrapper<>();
        catWrapper.select(Product::getCategory).eq(Product::getStatus, 1)
                .groupBy(Product::getCategory);
        List<String> categories = productMapper.selectList(catWrapper).stream()
                .map(Product::getCategory).distinct().collect(Collectors.toList());

        filters.put("brands", brands);
        filters.put("categories", categories);

        // 缓存 30 分钟
        redisUtil.set(CACHE_FILTERS, filters, 30, TimeUnit.MINUTES);
        return filters;
    }

    @Override
    public Page<Product> merchantPage(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Product::getStatus, 3);
        if (keyword != null && !keyword.isBlank()) wrapper.and(w -> w.like(Product::getName, keyword).or().like(Product::getBrand, keyword));
        return productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper.orderByDesc(Product::getUpdatedAt));
    }

    @Override
    @Transactional
    public Product create(MerchantProductSaveDTO dto) {
        Product product = new Product();
        copy(dto, product);
        product.setSales(0);
        product.setStatus(2);
        productMapper.insert(product);
        clearCache(product.getId());
        aiIndexTaskPublisher.publish(product.getId());
        return product;
    }

    @Override
    @Transactional
    public Product update(Long id, MerchantProductSaveDTO dto) {
        Product product = requireProduct(id);
        copy(dto, product);
        productMapper.updateById(product);
        clearCache(id);
        aiIndexTaskPublisher.publish(id);
        return product;
    }

    @Override
    @Transactional
    public Product updateStatus(Long id, Integer status) {
        Product product = requireProduct(id);
        if (status == 1 && productSkuMapper.selectCount(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, id).eq(ProductSku::getStatus, 1)) == 0) {
            throw new BizException(400, "上架商品前请至少创建一个可售 SKU");
        }
        product.setStatus(status);
        productMapper.updateById(product);
        clearCache(id);
        aiIndexTaskPublisher.publish(id);
        return product;
    }

    @Override
    @Transactional
    public Product copy(Long id) {
        Product source = requireProduct(id);
        Product copy = new Product();
        copy.setName(source.getName() + "（副本）");
        copy.setBrand(source.getBrand()); copy.setCategory(source.getCategory()); copy.setPrice(source.getPrice());
        copy.setOriginalPrice(source.getOriginalPrice()); copy.setSpecJson(source.getSpecJson());
        copy.setMainImage(source.getMainImage()); copy.setImages(source.getImages()); copy.setDescription(source.getDescription());
        copy.setColorOptions(source.getColorOptions()); copy.setStorageOptions(source.getStorageOptions());
        copy.setSales(0); copy.setStatus(2);
        productMapper.insert(copy);
        clearCache(copy.getId());
        aiIndexTaskPublisher.publish(copy.getId());
        return copy;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = requireProduct(id);
        product.setStatus(3);
        productMapper.updateById(product);
        clearCache(id);
        aiIndexTaskPublisher.publish(id);
    }

    @Override
    @Transactional
    public void updateBatchStatus(List<Long> ids, Integer status) {
        for (Long id : ids) updateStatus(id, status);
    }

    private Product requireProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == 3) throw new BizException(404, "商品不存在");
        return product;
    }

    private void copy(MerchantProductSaveDTO dto, Product product) {
        product.setName(dto.getName()); product.setBrand(dto.getBrand()); product.setCategory(dto.getCategory());
        product.setPrice(dto.getPrice()); product.setOriginalPrice(dto.getOriginalPrice()); product.setSpecJson(dto.getSpecJson());
        product.setMainImage(dto.getMainImage()); product.setImages(dto.getImages()); product.setDescription(dto.getDescription());
        product.setColorOptions(dto.getColorOptions()); product.setStorageOptions(dto.getStorageOptions());
    }

    private void clearCache(Long productId) {
        redisUtil.delete(CACHE_PRODUCT + productId); redisUtil.delete(CACHE_PRODUCT + productId + ":null");
        redisUtil.deleteByPattern(CACHE_HOT + ":*"); redisUtil.delete(CACHE_FILTERS);
    }

    @Override
    public List<ProductSku> listSkus(Long productId) {
        requireProduct(productId);
        return productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId).orderByDesc(ProductSku::getCreatedAt));
    }

    @Override
    public List<ProductSku> listAvailableSkus(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() != 1) {
            throw new BizException(404, "商品不存在或已下架");
        }
        List<ProductSku> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId)
                .eq(ProductSku::getStatus, 1)
                .orderByAsc(ProductSku::getCreatedAt));
        for (ProductSku sku : skus) {
            Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getSkuId, sku.getId()));
            sku.setAvailable(stock == null ? 0 : stock.getAvailable());
        }
        return skus;
    }

    @Override
    @Transactional
    public ProductSku createSku(Long productId, Long operatorId, MerchantSkuSaveDTO dto) {
        requireProduct(productId);
        if (productSkuMapper.selectCount(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getSkuCode, dto.getSkuCode())) > 0) {
            throw new BizException(400, "SKU编码已存在");
        }
        ProductSku sku = new ProductSku();
        sku.setProductId(productId); sku.setSkuCode(dto.getSkuCode()); sku.setSpecJson(dto.getSpecJson());
        sku.setPrice(dto.getPrice()); sku.setOriginalPrice(dto.getOriginalPrice()); sku.setStatus(1);
        productSkuMapper.insert(sku);
        Stock stock = new Stock();
        stock.setProductId(productId); stock.setSkuId(sku.getId()); stock.setTotal(dto.getInitialStock());
        stock.setLocked(0); stock.setAvailable(dto.getInitialStock()); stock.setVersion(0);
        stockMapper.insert(stock);
        syncStockCache(stock);
        StockLedger ledger = new StockLedger();
        ledger.setSkuId(sku.getId()); ledger.setAction("INITIAL_STOCK"); ledger.setQuantity(dto.getInitialStock());
        ledger.setBeforeTotal(0); ledger.setAfterTotal(dto.getInitialStock());
        ledger.setBeforeLocked(0); ledger.setAfterLocked(0);
        ledger.setBeforeAvailable(0); ledger.setAfterAvailable(dto.getInitialStock()); ledger.setOperatorId(operatorId);
        stockLedgerMapper.insert(ledger);
        clearCache(productId);
        return sku;
    }

    @Override
    public ProductSku updateSku(Long skuId, MerchantSkuUpdateDTO dto) {
        ProductSku sku = requireSku(skuId);
        if (!sku.getSkuCode().equals(dto.getSkuCode()) && productSkuMapper.selectCount(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getSkuCode, dto.getSkuCode())) > 0) {
            throw new BizException(400, "SKU编码已存在");
        }
        sku.setSkuCode(dto.getSkuCode());
        sku.setSpecJson(dto.getSpecJson());
        sku.setPrice(dto.getPrice());
        sku.setOriginalPrice(dto.getOriginalPrice());
        sku.setStatus(dto.getStatus());
        productSkuMapper.updateById(sku);
        clearCache(sku.getProductId());
        return sku;
    }

    @Override
    public Stock getSkuStock(Long skuId) {
        requireSku(skuId);
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getSkuId, skuId));
        if (stock == null) throw new BizException(404, "SKU库存不存在");
        return stock;
    }

    @Override
    @Transactional
    public Stock adjustSkuStock(Long skuId, Long operatorId, StockAdjustDTO dto) {
        if (dto.getQuantity() == 0) throw new BizException(400, "调整数量不能为0");
        ProductSku sku = requireSku(skuId);
        for (int attempt = 0; attempt < 3; attempt++) {
            Stock stock = getSkuStock(skuId);
            int nextAvailable = stock.getAvailable() + dto.getQuantity();
            int nextTotal = stock.getTotal() + dto.getQuantity();
            if (nextAvailable < 0 || nextTotal < stock.getLocked()) {
                throw new BizException(400, "调整后可用库存不能为负数");
            }
            int rows = stockMapper.update(null, new LambdaUpdateWrapper<Stock>()
                    .eq(Stock::getId, stock.getId()).eq(Stock::getVersion, stock.getVersion())
                    .set(Stock::getAvailable, nextAvailable).set(Stock::getTotal, nextTotal)
                    .set(Stock::getVersion, stock.getVersion() + 1));
            if (rows == 1) {
                StockLedger ledger = new StockLedger();
                ledger.setSkuId(skuId); ledger.setAction("MANUAL_ADJUST"); ledger.setQuantity(dto.getQuantity());
                ledger.setBeforeTotal(stock.getTotal()); ledger.setAfterTotal(nextTotal);
                ledger.setBeforeLocked(stock.getLocked()); ledger.setAfterLocked(stock.getLocked());
                ledger.setBeforeAvailable(stock.getAvailable()); ledger.setAfterAvailable(nextAvailable);
                ledger.setOperatorId(operatorId); ledger.setReferenceNo(dto.getReason());
                stockLedgerMapper.insert(ledger);
                clearCache(sku.getProductId());
                stock.setAvailable(nextAvailable); stock.setTotal(nextTotal); stock.setVersion(stock.getVersion() + 1);
                syncStockCache(stock);
                return stock;
            }
        }
        throw new BizException(409, "库存并发调整，请重试");
    }

    @Override
    public List<StockLedger> listSkuStockLedgers(Long skuId) {
        requireSku(skuId);
        return stockLedgerMapper.selectList(new LambdaQueryWrapper<StockLedger>()
                .eq(StockLedger::getSkuId, skuId).orderByDesc(StockLedger::getCreatedAt));
    }

    private ProductSku requireSku(Long skuId) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) throw new BizException(404, "SKU不存在");
        return sku;
    }

    private void syncStockCache(Stock stock) {
        String key = "stock:sku:" + stock.getSkuId();
        redisUtil.hSet(key, "total", stock.getTotal().toString());
        redisUtil.hSet(key, "locked", stock.getLocked().toString());
        redisUtil.hSet(key, "available", stock.getAvailable().toString());
    }
}
