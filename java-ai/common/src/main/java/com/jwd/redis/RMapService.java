package com.jwd.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RMapService {

    private final RedissonClient redissonClient;

    /**
     * 设置字段
     */
    public void set(String key, String field, String value) {
        redissonClient.getMap(key).put(field, value);
    }

    /**
     * 批量设置
     */
    public void setBatch(String key, Map<String, String> map){
        redissonClient.getMap(key).putAll(map);
    }

    /**
     * 获取字段
     */
    public String get(String key, String field) {
        return redissonClient.getMap(key).get(field).toString();
    }

    /**
     * 获取所有字段
     */
    public Map<String, String> getAll(String key) {
        return redissonClient.getMap(key);
    }

    /**
     * 判断字段是否存在
     */
    public boolean isExists(String key, String field) {
        return redissonClient.getMap(key).containsKey(field);
    }

    /**
     * 删除字段
     */
    public void delete(String key, String field) {
        redissonClient.getMap(key).fastRemove(field);
    }

    /**
     * 设置带过期时间的Map
     */
    public void setWithTTL(String key, String field, String value, int ttl, TimeUnit unit) {
        redissonClient.getMap(key).put(field, value);
        redissonClient.getMap(key).expire(ttl, unit);
    }

    /**
     * 存储Java对象
     */
    public void setObj(String key, Object value) {
        redissonClient.getMap(key).put(key, value);
    }

    /**
     * 获取Java对象
     */
    public <V> V getObj(String key) {
        return (V) redissonClient.getMap(key).get(key);
    }
}
