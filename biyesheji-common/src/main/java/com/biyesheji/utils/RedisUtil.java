package com.biyesheji.utils;

import cn.hutool.core.util.StrUtil;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 */
@Component
public class RedisUtil {

    private final RedissonClient redissonClient;

    public RedisUtil(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void set(String key, Object value) {
        redissonClient.getBucket(key).set(value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redissonClient.getBucket(key).set(value, Duration.ofMillis(unit.toMillis(timeout)));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    /**
     * SETNX - 仅当 key 不存在时设置（用于幂等/防重）
     */
    public boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return redissonClient.getBucket(key).setIfAbsent(value, Duration.ofMillis(unit.toMillis(timeout)));
    }

    public long increment(String key) {
        return redissonClient.getAtomicLong(key).incrementAndGet();
    }

    public long decrement(String key) {
        return redissonClient.getAtomicLong(key).decrementAndGet();
    }

    /**
     * 获取分布式锁
     */
    public RLock getLock(String key) {
        return redissonClient.getLock("lock:" + key);
    }

    /**
     * 执行 Lua 脚本（简化版，返回 Long）
     */
    public Long executeLua(String script, java.util.List<Object> keys, Object... args) {
        return redissonClient.getScript().eval(
                org.redisson.api.RScript.Mode.READ_WRITE,
                script,
                org.redisson.api.RScript.ReturnType.INTEGER,
                keys,
                args
        );
    }

    /**
     * Hash 操作
     */
    public void hSet(String key, String field, Object value) {
        redissonClient.getMap(key).put(field, value);
    }

    public Object hGet(String key, String field) {
        return redissonClient.getMap(key).get(field);
    }
}
