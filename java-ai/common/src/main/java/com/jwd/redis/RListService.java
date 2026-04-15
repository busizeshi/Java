/**
 * Redisson 操作 List
 */
package com.jwd.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RListService {

    private final RedissonClient redissonClient;

    /**
     * 从尾部添加元素
     */
    public boolean add(String key, String value) {
        return getList(key).add(value);
    }

    /**
     * 批量添加元素
     */
    public boolean addAll(String key, Collection<String> values) {
        return getList(key).addAll(values);
    }

    /**
     * 获取指定位置的元素
     */
    public String get(String key, int index) {
        return getList(key).get(index);
    }

    /**
     * 获取所有元素
     */
    public List<String> getAll(String key) {
        return getList(key).readAll();
    }

    /**
     * 获取列表大小
     */
    public int size(String key) {
        return getList(key).size();
    }

    /**
     * 判断是否包含元素
     */
    public boolean contains(String key, String value) {
        return getList(key).contains(value);
    }

    /**
     * 删除指定元素
     */
    public boolean remove(String key, String value) {
        return getList(key).remove(value);
    }

    /**
     * 从尾部弹出元素（栈效果）
     */
    public String popLast(String key) {
        RList<String> list = getList(key);
        if (list.isEmpty()) return null;
        return list.remove(list.size() - 1);
    }

    /**
     * 从头部弹出元素（队列效果）
     */
    public String popFirst(String key) {
        RList<String> list = getList(key);
        if (list.isEmpty()) return null;
        return list.remove(0);
    }

    /**
     * 保留指定范围的元素（截取）
     */
    public void trim(String key, int from, int to) {
        getList(key).trim(from, to);
    }

    /**
     * 替换指定位置的元素
     */
    public String set(String key, int index, String value) {
        return getList(key).set(index, value);
    }

    /**
     * 删除整个列表
     */
    public void delete(String key) {
        getList(key).delete();
    }

    /**
     * 获取 RList 对象
     */
    private RList<String> getList(String key) {
        return redissonClient.getList(key);
    }
}
