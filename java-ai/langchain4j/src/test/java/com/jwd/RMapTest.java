package com.jwd;

import com.jwd.redis.RMapService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RMapTest {

    @Autowired
    private RMapService rMapService;

    @Autowired
    private RedissonClient redissonClient;

    @BeforeEach
    void cleanOldData() {
        // 清理之前序列化格式冲突的旧数据（内部类 TestUser -> 独立类 TestUser）
        redissonClient.getBucket("test:obj:user:1").delete();
    }

    @Test
    @Order(1)
    @DisplayName("测试 set 方法 - 设置单个字段")
    public void testSet() {
        rMapService.set("test:user:1", "name", "张三");
        rMapService.set("test:user:1", "age", "20");
        rMapService.set("test:user:1", "email", "zhangsan@example.com");
        System.out.println("set 成功");
    }

    @Test
    @Order(2)
    @DisplayName("测试 get 方法 - 获取单个字段")
    public void testGet() {
        String name = rMapService.get("test:user:1", "name");
        Assertions.assertEquals("张三", name);
        System.out.println("get name = " + name);

        String age = rMapService.get("test:user:1", "age");
        Assertions.assertEquals("20", age);
        System.out.println("get age = " + age);
    }

    @Test
    @Order(3)
    @DisplayName("测试 setBatch 方法 - 批量设置字段")
    public void testSetBatch() {
        Map<String, String> data = new HashMap<>();
        data.put("city", "北京");
        data.put("district", "朝阳");
        data.put("street", "望京街道");
        rMapService.setBatch("test:address:1", data);
        System.out.println("setBatch 成功");
    }

    @Test
    @Order(4)
    @DisplayName("测试 getAll 方法 - 获取所有字段")
    public void testGetAll() {
        Map<String, String> all = rMapService.getAll("test:user:1");
        Assertions.assertEquals("张三", all.get("name"));
        Assertions.assertEquals("20", all.get("age"));
        Assertions.assertEquals("zhangsan@example.com", all.get("email"));
        System.out.println("getAll = " + all);
    }

    @Test
    @Order(5)
    @DisplayName("测试 isExists 方法 - 判断字段是否存在")
    public void testIsExists() {
        Assertions.assertTrue(rMapService.isExists("test:user:1", "name"));
        Assertions.assertFalse(rMapService.isExists("test:user:1", "phone"));
        System.out.println("isExists 测试通过");
    }

    @Test
    @Order(6)
    @DisplayName("测试 delete 方法 - 删除字段")
    public void testDelete() {
        rMapService.set("test:user:del", "temp", "临时数据");
        Assertions.assertTrue(rMapService.isExists("test:user:del", "temp"));

        rMapService.delete("test:user:del", "temp");
        Assertions.assertFalse(rMapService.isExists("test:user:del", "temp"));
        System.out.println("delete 成功");
    }

    @Test
    @Order(7)
    @DisplayName("测试 setWithTTL 方法 - 设置带过期时间的 Map")
    public void testSetWithTTL() {
        rMapService.setWithTTL("test:session:1", "token", "abc123", 10, TimeUnit.SECONDS);
        String token = rMapService.get("test:session:1", "token");
        Assertions.assertEquals("abc123", token);
        System.out.println("setWithTTL 成功: token=" + token);
    }

    @Test
    @Order(8)
    @DisplayName("测试 setObj / getObj 方法 - 存储和获取 Java 对象")
    public void testObj() {
        TestUser user = new TestUser(1L, "李四", 25);
        rMapService.setObj("test:obj:user:1", user);

        TestUser result = rMapService.getObj("test:obj:user:1");
        Assertions.assertNotNull(result);
        Assertions.assertEquals("李四", result.getName());
        Assertions.assertEquals(25, result.getAge());
        System.out.println("setObj/getObj 成功: " + result);
    }
}
