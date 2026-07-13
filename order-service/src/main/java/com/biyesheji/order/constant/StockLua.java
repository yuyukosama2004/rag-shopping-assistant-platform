package com.biyesheji.order.constant;

public final class StockLua {

    private StockLua() {
    }

    public static final String STOCK_DEDUCT = """
            local quantity = tonumber(ARGV[1])
            if not quantity or quantity <= 0 then return 0 end
            local available = redis.call('HGET', KEYS[1], 'available')
            if not available or tonumber(available) < quantity then return 0 end
            redis.call('HINCRBY', KEYS[1], 'locked', quantity)
            redis.call('HINCRBY', KEYS[1], 'available', -quantity)
            return 1
            """;

    public static final String STOCK_RESTORE = """
            local quantity = tonumber(ARGV[1])
            if not quantity or quantity <= 0 then return 0 end
            local locked = redis.call('HGET', KEYS[1], 'locked')
            if not locked or tonumber(locked) < quantity then return 0 end
            redis.call('HINCRBY', KEYS[1], 'locked', -quantity)
            redis.call('HINCRBY', KEYS[1], 'available', quantity)
            return 1
            """;

    public static final String STOCK_CONFIRM = """
            local quantity = tonumber(ARGV[1])
            if not quantity or quantity <= 0 then return 0 end
            local total = redis.call('HGET', KEYS[1], 'total')
            local locked = redis.call('HGET', KEYS[1], 'locked')
            if not total or not locked or tonumber(total) < quantity or tonumber(locked) < quantity then return 0 end
            redis.call('HINCRBY', KEYS[1], 'total', -quantity)
            redis.call('HINCRBY', KEYS[1], 'locked', -quantity)
            return 1
            """;
}
