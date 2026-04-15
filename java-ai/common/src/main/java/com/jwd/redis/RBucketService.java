/**
 * redission 操作 String
 */
package com.jwd.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class RBucketService {

    private final RedissonClient redissonClient;

    /**
     * 设置字符串
     * @param key 键
     * @param value 值
     */
    public void set(String key, String value) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }

    /**
     * 获取字符串
     * @param key 键
     * @return 值
     */
    public String get(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    /**
     * 设置字符串并设置过期时间
     * @param key 键
     * @param value 值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(value, timeout, unit);
    }

    /**
     * 删除字符串
     * @param key 键
     */
    public void delete(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.delete();
    }

    /**
     * 判断字符串是否存在
     * @param key 键
     * @return 是否存在
     */
    public boolean isExists(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.isExists();
    }

    /**
     * 尝试设置字符串
     * @param key 键
     * @param value 值
     * @return 是否设置成功
     */
    public boolean trySet(String key, String value) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.trySet(value);
    }

    /**
     * 获取并设置字符串
     * @param key 键
     * @param value 值
     * @return 原值
     */
    public String getAndSet(String key, String value) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.getAndSet(value);
    }

    /**
     * 存储对象
     */
    public void setObj(String key, Object value) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }

    /**
     * 获取对象
     */
    public <T> T getObj(String key, Class<T> clazz) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        return clazz.cast(bucket.get());
    }
}
