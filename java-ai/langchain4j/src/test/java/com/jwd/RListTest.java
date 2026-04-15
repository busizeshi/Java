package com.jwd;

import com.jwd.redis.RListService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RListTest {

    @Autowired
    private RListService rListService;

    @AfterEach
    void cleanup() {
        rListService.delete("test:list:users");
        rListService.delete("test:list:stack");
        rListService.delete("test:list:queue");
        rListService.delete("test:list:trim");
        rListService.delete("test:list:del");
        rListService.delete("test:list:expire");
    }

    @Test
    @Order(1)
    @DisplayName("测试 add 方法 - 添加对象")
    public void testAdd() {
        rListService.add("test:list:users", new TestUser(1L, "张三", 20));
        rListService.add("test:list:users", new TestUser(2L, "李四", 25));
        rListService.add("test:list:users", new TestUser(3L, "王五", 30));

        Assertions.assertEquals(3, rListService.size("test:list:users"));
        System.out.println("add 成功，列表大小: " + rListService.size("test:list:users"));
    }

    @Test
    @Order(2)
    @DisplayName("测试 addAll 方法 - 批量添加对象")
    public void testAddAll() {
        rListService.add("test:list:users", new TestUser(1L, "张三", 20));
        rListService.add("test:list:users", new TestUser(2L, "李四", 25));
        rListService.addAll("test:list:users", Arrays.asList(
                new TestUser(3L, "王五", 30),
                new TestUser(4L, "赵六", 35)
        ));
        Assertions.assertEquals(4, rListService.size("test:list:users"));
        System.out.println("addAll 成功，列表大小: " + rListService.size("test:list:users"));
    }

    @Test
    @Order(3)
    @DisplayName("测试 get 方法 - 获取指定位置对象")
    public void testGet() {
        rListService.add("test:list:users", new TestUser(1L, "张三", 20));
        rListService.add("test:list:users", new TestUser(2L, "李四", 25));

        TestUser user = rListService.get("test:list:users", 0);
        Assertions.assertNotNull(user);
        Assertions.assertEquals("张三", user.getName());
        Assertions.assertEquals(20, user.getAge());
        System.out.println("get index=0: " + user);
    }

    @Test
    @Order(4)
    @DisplayName("测试 getAll 方法 - 获取所有对象")
    public void testGetAll() {
        rListService.add("test:list:users", new TestUser(1L, "张三", 20));
        rListService.add("test:list:users", new TestUser(2L, "李四", 25));
        rListService.add("test:list:users", new TestUser(3L, "王五", 30));

        List<TestUser> all = rListService.getAll("test:list:users");
        Assertions.assertEquals(3, all.size());
        Assertions.assertEquals("张三", all.get(0).getName());
        Assertions.assertEquals("李四", all.get(1).getName());
        System.out.println("getAll: " + all);
    }

    @Test
    @Order(5)
    @DisplayName("测试 contains 方法 - 判断是否包含对象")
    public void testContains() {
        TestUser user1 = new TestUser(1L, "张三", 20);
        rListService.add("test:list:users", user1);
        rListService.add("test:list:users", new TestUser(2L, "李四", 25));

        Assertions.assertTrue(rListService.contains("test:list:users", user1));
        Assertions.assertFalse(rListService.contains("test:list:users", new TestUser(99L, "不存在", 99)));
        System.out.println("contains 测试通过");
    }

    @Test
    @Order(6)
    @DisplayName("测试 set 方法 - 替换指定位置对象")
    public void testSet() {
        rListService.add("test:list:users", new TestUser(1L, "张三", 20));
        rListService.add("test:list:users", new TestUser(2L, "李四", 25));
        rListService.add("test:list:users", new TestUser(3L, "王五", 30));

        TestUser newUser = new TestUser(99L, "替换者", 100);
        TestUser oldUser = rListService.set("test:list:users", 1, newUser);

        Assertions.assertEquals("李四", oldUser.getName());
        TestUser newUserResult = rListService.get("test:list:users", 1);
        Assertions.assertEquals("替换者", newUserResult.getName());
        System.out.println("set 测试通过: 旧=" + oldUser.getName() + ", 新=" + newUserResult);
    }

    @Test
    @Order(7)
    @DisplayName("测试 remove 方法 - 删除指定对象")
    public void testRemove() {
        TestUser user = new TestUser(1L, "张三", 20);
        rListService.add("test:list:users", user);
        rListService.add("test:list:users", new TestUser(2L, "李四", 25));

        rListService.remove("test:list:users", user);
        Assertions.assertFalse(rListService.contains("test:list:users", user));
        Assertions.assertEquals(1, rListService.size("test:list:users"));
        System.out.println("remove 成功");
    }

    @Test
    @Order(8)
    @DisplayName("测试 popLast 方法 - 从尾部弹出（栈效果）")
    public void testPopLast() {
        rListService.add("test:list:stack", new TestUser(1L, "A", 10));
        rListService.add("test:list:stack", new TestUser(2L, "B", 20));
        rListService.add("test:list:stack", new TestUser(3L, "C", 30));

        TestUser last = rListService.popLast("test:list:stack");
        Assertions.assertNotNull(last);
        Assertions.assertEquals("C", last.getName());
        Assertions.assertEquals(2, rListService.size("test:list:stack"));
        System.out.println("popLast: " + last + ", 剩余大小: " + rListService.size("test:list:stack"));
    }

    @Test
    @Order(9)
    @DisplayName("测试 popFirst 方法 - 从头部弹出（队列效果）")
    public void testPopFirst() {
        rListService.add("test:list:queue", new TestUser(1L, "First", 1));
        rListService.add("test:list:queue", new TestUser(2L, "Second", 2));
        rListService.add("test:list:queue", new TestUser(3L, "Third", 3));

        TestUser first = rListService.popFirst("test:list:queue");
        Assertions.assertNotNull(first);
        Assertions.assertEquals("First", first.getName());
        Assertions.assertEquals(2, rListService.size("test:list:queue"));
        System.out.println("popFirst: " + first + ", 剩余大小: " + rListService.size("test:list:queue"));
    }

    @Test
    @Order(10)
    @DisplayName("测试 trim 方法 - 截取列表")
    public void testTrim() {
        rListService.add("test:list:trim", new TestUser(1L, "A", 1));
        rListService.add("test:list:trim", new TestUser(2L, "B", 2));
        rListService.add("test:list:trim", new TestUser(3L, "C", 3));
        rListService.add("test:list:trim", new TestUser(4L, "D", 4));
        rListService.add("test:list:trim", new TestUser(5L, "E", 5));

        rListService.trim("test:list:trim", 0, 2);
        Assertions.assertEquals(3, rListService.size("test:list:trim"));
        List<TestUser> trimmed = rListService.getAll("test:list:trim");
        System.out.println("trim 成功: " + trimmed);
    }

    @Test
    @Order(11)
    @DisplayName("测试 delete 方法 - 删除整个列表")
    public void testDelete() {
        rListService.add("test:list:del", new TestUser(1L, "待删除", 100));
        Assertions.assertFalse(rListService.getAll("test:list:del").isEmpty());

        rListService.delete("test:list:del");
        Assertions.assertEquals(0, rListService.size("test:list:del"));
        System.out.println("delete 成功");
    }

    @Test
    @Order(12)
    @DisplayName("测试 expire 方法 - 设置列表过期时间")
    public void testExpire() {
        rListService.add("test:list:expire", new TestUser(1L, "临时用户", 18));
        Assertions.assertEquals(1, rListService.size("test:list:expire"));

        // 设置 5 秒过期
        rListService.expire("test:list:expire", 5, TimeUnit.SECONDS);

        // 立即查询还在
        Assertions.assertEquals(1, rListService.size("test:list:expire"));
        System.out.println("expire 设置成功，等待过期...");
    }
}
