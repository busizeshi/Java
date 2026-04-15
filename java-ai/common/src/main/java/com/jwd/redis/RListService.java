/**
 * Redisson 操作 List（支持存储任意 Java 对象）
 */
package com.jwd.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RListService {

    private final RedissonClient redissonClient;

    /**
     * 从尾部添加对象
     */
    @SuppressWarnings("unchecked")
    public <T> boolean add(String key, T value) {
        return ((RList<T>) getList(key)).add(value);
    }

    /**
     * 批量添加对象
     */
    @SuppressWarnings("unchecked")
    public <T> boolean addAll(String key, Collection<T> values) {
        return ((RList<T>) getList(key)).addAll(values);
    }

    /**
     * 获取指定位置的对象
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, int index) {
        return ((RList<T>) getList(key)).get(index);
    }

    /**
     * 获取所有对象
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getAll(String key) {
        return ((RList<T>) getList(key)).readAll();
    }

    /**
     * 获取列表大小
     */
    public int size(String key) {
        return getList(key).size();
    }

    /**
     * 判断是否包含对象
     */
    @SuppressWarnings("unchecked")
    public <T> boolean contains(String key, T value) {
        return ((RList<T>) getList(key)).contains(value);
    }

    /**
     * 删除指定对象
     */
    @SuppressWarnings("unchecked")
    public <T> boolean remove(String key, T value) {
        return ((RList<T>) getList(key)).remove(value);
    }

    /**
     * 从尾部弹出对象（栈效果）
     */
    @SuppressWarnings("unchecked")
    public <T> T popLast(String key) {
        RList<T> list = (RList<T>) getList(key);
        if (list.isEmpty()) return null;
        return list.remove(list.size() - 1);
    }

    /**
     * 从头部弹出对象（队列效果）
     */
    @SuppressWarnings("unchecked")
    public <T> T popFirst(String key) {
        RList<T> list = (RList<T>) getList(key);
        if (list.isEmpty()) return null;
        return list.remove(0);
    }

    /**
     * 保留指定范围的对象（截取）
     */
    public void trim(String key, int from, int to) {
        getList(key).trim(from, to);
    }

    /**
     * 替换指定位置的对象
     */
    @SuppressWarnings("unchecked")
    public <T> T set(String key, int index, T value) {
        return ((RList<T>) getList(key)).set(index, value);
    }

    /**
     * 删除整个列表
     */
    public void delete(String key) {
        getList(key).delete();
    }

    /**
     * 设置列表过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return getList(key).expire(timeout, unit);
    }

    /**
     * 获取 RList 对象
     */
    private <T> RList<T> getList(String key) {
        return redissonClient.getList(key);
    }
}
