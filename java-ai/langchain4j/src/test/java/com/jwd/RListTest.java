package com.jwd;

import com.jwd.redis.RListService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RListTest {

    @Autowired
    private RListService rListService;

    @AfterEach
    void cleanup() {
        rListService.delete("test:tags");
        rListService.delete("test:stack");
        rListService.delete("test:queue");
        rListService.delete("test:trim");
        rListService.delete("test:del");
    }

    @Test
    @Order(1)
    @DisplayName("测试 add 方法 - 从尾部添加元素")
    public void testAdd() {
        rListService.add("test:tags", "Java");
        rListService.add("test:tags", "Python");
        rListService.add("test:tags", "Go");
        Assertions.assertEquals(3, rListService.size("test:tags"));
        System.out.println("add 成功，列表大小: " + rListService.size("test:tags"));
    }

    @Test
    @Order(2)
    @DisplayName("测试 addAll 方法 - 批量添加元素")
    public void testAddAll() {
        rListService.add("test:tags", "Java");
        rListService.add("test:tags", "Python");
        rListService.addAll("test:tags", Arrays.asList("C++", "Rust", "Go"));
        Assertions.assertEquals(5, rListService.size("test:tags"));
        System.out.println("addAll 成功，列表大小: " + rListService.size("test:tags"));
    }

    @Test
    @Order(3)
    @DisplayName("测试 get 方法 - 获取指定位置元素")
    public void testGet() {
        rListService.add("test:tags", "Java");
        rListService.add("test:tags", "Python");
        rListService.add("test:tags", "Go");

        String first = rListService.get("test:tags", 0);
        Assertions.assertEquals("Java", first);
        System.out.println("get index=0: " + first);

        String second = rListService.get("test:tags", 1);
        Assertions.assertEquals("Python", second);
        System.out.println("get index=1: " + second);
    }

    @Test
    @Order(4)
    @DisplayName("测试 getAll 方法 - 获取所有元素")
    public void testGetAll() {
        rListService.add("test:tags", "Java");
        rListService.add("test:tags", "Python");
        rListService.add("test:tags", "Go");

        var all = rListService.getAll("test:tags");
        Assertions.assertEquals(3, all.size());
        System.out.println("getAll: " + all);
    }

    @Test
    @Order(5)
    @DisplayName("测试 contains 方法 - 判断是否包含元素")
    public void testContains() {
        rListService.add("test:tags", "Java");
        rListService.add("test:tags", "Python");

        Assertions.assertTrue(rListService.contains("test:tags", "Java"));
        Assertions.assertFalse(rListService.contains("test:tags", "Ruby"));
        System.out.println("contains 测试通过");
    }

    @Test
    @Order(6)
    @DisplayName("测试 set 方法 - 替换指定位置元素")
    public void testSet() {
        rListService.add("test:tags", "Java");
        rListService.add("test:tags", "Python");
        rListService.add("test:tags", "Go");

        String old = rListService.set("test:tags", 1, "JavaScript");
        Assertions.assertEquals("Python", old);
        Assertions.assertEquals("JavaScript", rListService.get("test:tags", 1));
        System.out.println("set 测试通过: 旧值=" + old + ", 新值=" + rListService.get("test:tags", 1));
    }

    @Test
    @Order(7)
    @DisplayName("测试 remove 方法 - 删除指定元素")
    public void testRemove() {
        rListService.add("test:tags", "Java");
        rListService.add("test:tags", "Python");
        rListService.add("test:tags", "Go");

        rListService.remove("test:tags", "Go");
        Assertions.assertFalse(rListService.contains("test:tags", "Go"));
        System.out.println("remove 成功");
    }

    @Test
    @Order(8)
    @DisplayName("测试 popLast 方法 - 从尾部弹出（栈效果）")
    public void testPopLast() {
        rListService.add("test:stack", "A");
        rListService.add("test:stack", "B");
        rListService.add("test:stack", "C");

        String last = rListService.popLast("test:stack");
        Assertions.assertEquals("C", last);
        Assertions.assertEquals(2, rListService.size("test:stack"));
        System.out.println("popLast: " + last + ", 剩余大小: " + rListService.size("test:stack"));
    }

    @Test
    @Order(9)
    @DisplayName("测试 popFirst 方法 - 从头部弹出（队列效果）")
    public void testPopFirst() {
        rListService.add("test:queue", "1");
        rListService.add("test:queue", "2");
        rListService.add("test:queue", "3");

        String first = rListService.popFirst("test:queue");
        Assertions.assertEquals("1", first);
        Assertions.assertEquals(2, rListService.size("test:queue"));
        System.out.println("popFirst: " + first + ", 剩余大小: " + rListService.size("test:queue"));
    }

    @Test
    @Order(10)
    @DisplayName("测试 trim 方法 - 截取列表")
    public void testTrim() {
        rListService.add("test:trim", "A");
        rListService.add("test:trim", "B");
        rListService.add("test:trim", "C");
        rListService.add("test:trim", "D");
        rListService.add("test:trim", "E");

        rListService.trim("test:trim", 0, 2);
        Assertions.assertEquals(3, rListService.size("test:trim"));
        System.out.println("trim 成功: " + rListService.getAll("test:trim"));
    }

    @Test
    @Order(11)
    @DisplayName("测试 delete 方法 - 删除整个列表")
    public void testDelete() {
        rListService.add("test:del", "x");
        Assertions.assertFalse(rListService.getAll("test:del").isEmpty());

        rListService.delete("test:del");
        Assertions.assertEquals(0, rListService.size("test:del"));
        System.out.println("delete 成功");
    }
}
