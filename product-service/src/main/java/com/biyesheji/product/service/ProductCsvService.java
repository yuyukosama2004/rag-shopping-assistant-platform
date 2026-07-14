package com.biyesheji.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.dto.MerchantProductSaveDTO;
import com.biyesheji.dto.MerchantSkuSaveDTO;
import com.biyesheji.entity.Product;
import com.biyesheji.entity.ProductSku;
import com.biyesheji.exception.BizException;
import com.biyesheji.product.mapper.ProductMapper;
import com.biyesheji.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductCsvService {
    public static final String HEADER = "productKey,name,brand,category,price,originalPrice,description,mainImage,skuCode,specJson,skuPrice,skuOriginalPrice,initialStock";
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductService productService;

    @Transactional
    public Map<String, Object> importCsv(Long operatorId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 2 * 1024 * 1024) throw new BizException(400, "CSV不能为空且不能超过2MiB");
        List<Row> rows = parse(file);
        List<Map<String, Object>> errors = validate(rows);
        if (!errors.isEmpty()) return Map.of("products", 0, "skus", 0, "errors", errors);
        Map<String, List<Row>> groups = new LinkedHashMap<>();
        for (Row row : rows) groups.computeIfAbsent(row.productKey, k -> new ArrayList<>()).add(row);
        for (List<Row> group : groups.values()) {
            Row first = group.get(0);
            MerchantProductSaveDTO product = new MerchantProductSaveDTO();
            product.setName(first.name); product.setBrand(first.brand); product.setCategory(first.category);
            product.setPrice(first.price); product.setOriginalPrice(first.originalPrice); product.setDescription(first.description); product.setMainImage(first.mainImage);
            Product created = productService.create(product);
            for (Row row : group) {
                MerchantSkuSaveDTO sku = new MerchantSkuSaveDTO();
                sku.setSkuCode(row.skuCode); sku.setSpecJson(row.specJson); sku.setPrice(row.skuPrice); sku.setOriginalPrice(row.skuOriginalPrice); sku.setInitialStock(row.initialStock);
                productService.createSku(created.getId(), operatorId, sku);
            }
        }
        return Map.of("products", groups.size(), "skus", rows.size(), "errors", List.of());
    }

    public byte[] exportCsv() {
        StringBuilder csv = new StringBuilder(HEADER).append('\n');
        for (Product product : productMapper.selectList(new LambdaQueryWrapper<Product>().ne(Product::getStatus, 3))) {
            List<ProductSku> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, product.getId()));
            for (ProductSku sku : skus) {
                csv.append(escape(String.valueOf(product.getId()))).append(',').append(escape(product.getName())).append(',').append(escape(product.getBrand())).append(',').append(escape(product.getCategory())).append(',')
                        .append(product.getPrice()).append(',').append(value(product.getOriginalPrice())).append(',').append(escape(product.getDescription())).append(',').append(escape(product.getMainImage())).append(',')
                        .append(escape(sku.getSkuCode())).append(',').append(escape(sku.getSpecJson())).append(',').append(sku.getPrice()).append(',').append(value(sku.getOriginalPrice())).append(',').append(0).append('\n');
            }
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<Row> parse(MultipartFile file) {
        try {
            List<String> lines = Arrays.asList(new String(file.getBytes(), StandardCharsets.UTF_8).replace("\r", "").split("\n"));
            if (lines.isEmpty() || !HEADER.equals(lines.get(0).replace("\uFEFF", ""))) throw new BizException(400, "CSV表头不匹配，请先导出模板");
            List<Row> rows = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) if (!lines.get(i).isBlank()) rows.add(Row.of(i + 1, columns(lines.get(i))));
            if (rows.isEmpty()) throw new BizException(400, "CSV没有数据行");
            return rows;
        } catch (java.io.IOException e) { throw new BizException(400, "CSV读取失败"); }
    }

    private List<Map<String, Object>> validate(List<Row> rows) {
        List<Map<String, Object>> errors = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        for (Row row : rows) {
            if (row.error != null) { errors.add(error(row.line, row.error)); continue; }
            if (row.productKey.isBlank() || row.name.isBlank() || row.brand.isBlank() || row.category.isBlank() || row.skuCode.isBlank() || row.price == null || row.skuPrice == null || row.initialStock == null || row.initialStock < 0 || row.price.signum() <= 0 || row.skuPrice.signum() <= 0) {
                errors.add(error(row.line, "字段无效")); continue;
            }
            if (!codes.add(row.skuCode) || productSkuMapper.selectCount(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getSkuCode, row.skuCode)) > 0) {
                errors.add(error(row.line, "SKU编码重复: " + row.skuCode));
            }
        }
        return errors;
    }

    private static Map<String, Object> error(int line, String message) { return Map.of("line", line, "message", message); }

    private static List<String> columns(String line) {
        List<String> values = new ArrayList<>(); StringBuilder value = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"' && quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { value.append(c); i++; }
            else if (c == '"') quoted = !quoted;
            else if (c == ',' && !quoted) { values.add(value.toString()); value.setLength(0); }
            else value.append(c);
        }
        if (quoted) return null;
        values.add(value.toString()); return values;
    }
    private static String value(BigDecimal value) { return value == null ? "" : value.toPlainString(); }
    private static String escape(String value) { return value == null ? "" : "\"" + value.replace("\"", "\"\"") + "\""; }
    private record Row(int line, String productKey, String name, String brand, String category, BigDecimal price, BigDecimal originalPrice, String description, String mainImage, String skuCode, String specJson, BigDecimal skuPrice, BigDecimal skuOriginalPrice, Integer initialStock, String error) {
        static Row of(int line, List<String> c) {
            if (c == null) return invalid(line, "CSV存在未闭合引号");
            if (c.size() != 13) return invalid(line, "列数错误");
            try { return new Row(line, c.get(0).trim(), c.get(1).trim(), c.get(2).trim(), c.get(3).trim(), decimal(c.get(4)), decimal(c.get(5)), c.get(6), c.get(7), c.get(8).trim(), c.get(9), decimal(c.get(10)), decimal(c.get(11)), Integer.valueOf(c.get(12).trim()), null); }
            catch (RuntimeException e) { return invalid(line, "数值格式错误"); }
        }
        static Row invalid(int line, String error) { return new Row(line, "", "", "", "", null, null, "", "", "", "", null, null, null, error); }
        static BigDecimal decimal(String value) { return value == null || value.isBlank() ? null : new BigDecimal(value.trim()); }
    }
}
