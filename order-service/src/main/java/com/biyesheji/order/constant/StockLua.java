package com.biyesheji.order.constant;

/**
 * 库存预扣 Lua 脚本
 * KEYS[1] = "stock:product:{id}" hash key
 * ARGV[1] = quantity
 * 返回: 1-预扣成功, 0-库存不足
 */
public class StockLua {

    public static final String STOCK_DEDUCT =
            "local key = KEYS[1]\n" +
            "local quantity = tonumber(ARGV[1])\n" +
            "local available = redis.call('HGET', key, 'available')\n" +
            "if not available then return 0 end\n" +
            "available = tonumber(available)\n" +
            "if available >= quantity then\n" +
            "    redis.call('HINCRBY', key, 'locked', quantity)\n" +
            "    redis.call('HINCRBY', key, 'available', -quantity)\n" +
            "    return 1\n" +
            "else\n" +
            "    return 0\n" +
            "end";

    public static final String STOCK_RESTORE =
            "local key = KEYS[1]\n" +
            "local quantity = tonumber(ARGV[1])\n" +
            "redis.call('HINCRBY', key, 'locked', -quantity)\n" +
            "redis.call('HINCRBY', key, 'available', quantity)\n" +
            "return 1";
}
