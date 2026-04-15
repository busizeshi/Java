package com.jwd;

import com.jwd.redis.RBucketService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RBucketTest {

    @Autowired
    private RBucketService rBucketService;

    @Test
    @Order(1)
    @DisplayName("测试 set 方法 - 设置字符串值")
    public void testSet() {
        rBucketService.set("test:name", "张三");
        rBucketService.set("test:age", "20");
        System.out.println("set 成功");
    }

    @Test
    @Order(2)
    @DisplayName("测试 get 方法 - 获取字符串值")
    public void testGet() {
        String name = rBucketService.get("test:name");
        Assertions.assertEquals("张三", name);
        System.out.println("get test:name = " + name);

        String age = rBucketService.get("test:age");
        Assertions.assertEquals("20", age);
        System.out.println("get test:age = " + age);
    }

    @Test
    @Order(3)
    @DisplayName("测试 get 方法 - 不存在的 key 返回 null")
    public void testGetNull() {
        String result = rBucketService.get("test:not_exist");
        Assertions.assertNull(result);
        System.out.println("不存在的 key 返回: " + result);
    }

    @Test
    @Order(4)
    @DisplayName("测试 set 带过期时间")
    public void testSetWithExpire() {
        rBucketService.set("test:expire", "临时数据", 10, java.util.concurrent.TimeUnit.SECONDS);
        String value = rBucketService.get("test:expire");
        Assertions.assertEquals("临时数据", value);
        System.out.println("set 带过期时间成功: " + value);
    }

    @Test
    @Order(5)
    @DisplayName("测试 delete 方法 - 删除 key")
    public void testDelete() {
        rBucketService.set("test:del", "待删除");
        Assertions.assertEquals("待删除", rBucketService.get("test:del"));

        rBucketService.delete("test:del");
        Assertions.assertNull(rBucketService.get("test:del"));
        System.out.println("delete 成功");
    }

    @Test
    @Order(6)
    @DisplayName("测试 isExists 方法 - 判断 key 是否存在")
    public void testIsExists() {
        rBucketService.set("test:exists", "存在");
        Assertions.assertTrue(rBucketService.isExists("test:exists"));
        Assertions.assertFalse(rBucketService.isExists("test:not_exists"));
        System.out.println("isExists 测试通过");
    }

    @Test
    @Order(7)
    @DisplayName("测试 trySet 方法 - 不存在时设置（setnx）")
    public void testTrySet() {
        // 首次设置，应该成功
        boolean first = rBucketService.trySet("test:tryset", "第一次");
        Assertions.assertTrue(first);

        // 再次设置，应该失败
        boolean second = rBucketService.trySet("test:tryset", "第二次");
        Assertions.assertFalse(second);

        // 值应该还是第一次设置的
        Assertions.assertEquals("第一次", rBucketService.get("test:tryset"));
        System.out.println("trySet 测试通过");
    }

    @Test
    @Order(8)
    @DisplayName("测试 getAndSet 方法 - 获取旧值并设置新值")
    public void testGetAndSet() {
        rBucketService.set("test:oldnew", "旧值");
        String oldValue = rBucketService.getAndSet("test:oldnew", "新值");
        Assertions.assertEquals("旧值", oldValue);
        Assertions.assertEquals("新值", rBucketService.get("test:oldnew"));
        System.out.println("getAndSet 测试通过: 旧值=" + oldValue + ", 新值=" + rBucketService.get("test:oldnew"));
    }

    @Test
    @Order(9)
    @DisplayName("测试 setObj 方法 - 设置对象值")
    public void testSetObj() {
        Person person = new Person("张三", 20);
        rBucketService.setObj("test:person", person);
        Person result = rBucketService.getObj("test:person", Person.class);
        Assertions.assertEquals(person.getName(), result.getName());
        Assertions.assertEquals(person.getAge(), result.getAge());
        System.out.println("setObj 测试通过: " + result);
    }

    @Test
    @Order(10)
    @DisplayName("测试 getObj 方法 - 获取对象值")
    public void testGetObj() {
        Person person = rBucketService.getObj("test:person", Person.class);
        Assertions.assertEquals("张三", person.getName());
        Assertions.assertEquals(20, person.getAge());
        System.out.println("getObj 测试通过: " + person);
    }

    public static class Person {
        private String name;
        private int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}
