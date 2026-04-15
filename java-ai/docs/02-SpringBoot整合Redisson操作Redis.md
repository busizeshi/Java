# Spring Boot 整合 Redisson 操作 Redis

> 本文档讲解在 Spring Boot 项目中使用 Redisson 客户端操作 Redis 的各种常见场景

---

## 一、为什么选择 Redisson？

| 特性 | Spring Data Redis (Lettuce/Jedis) | Redisson |
|------|------|------|
| API 风格 | 底层命令式 | 面向对象，类似 JDK 集合 |
| 分布式锁 | 需手动实现 | 内置多种锁 |
| 限流/布隆过滤器 | 需手写 Lua | 开箱即用 |
| 异步/响应式 | 支持 | 支持，且更完善 |
| 学习成本 | 低（熟悉 Redis 命令即可） | 低（会用 Java 集合就会用） |

**Redisson 的核心优势：** 把 Redis 的操作抽象成 Java 对象，用熟悉的 `Map`、`List`、`Set`、`Queue` 等方式操作 Redis。

---

## 二、引入依赖

在 `langchain4j/pom.xml`（或你的模块 pom）中添加：

```xml
<!-- Redisson Spring Boot Starter -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.40.2</version>
</dependency>
```

> Redisson 的 starter 会自动配置 `RedissonClient` Bean，无需手动创建。

### 移除 Spring Data Redis（可选）

如果项目中不再需要 `spring-boot-starter-data-redis`，可以移除，避免依赖冲突。Redisson 内部自带连接驱动。

---

## 三、配置文件

### 3.1 单节点模式（最常用）

```yaml
spring:
  data:
    redis:
      host: 115.190.125.94
      port: 6379
      password: ${REDIS_PASSWORD:}
      database: 0

# Redisson 专属配置（优先级更高，更精细）
redisson:
  single-server-config:
    address: redis://115.190.125.94:6379
    password: ${REDIS_PASSWORD:}
    database: 0
    # 连接池配置
    connection-pool-size: 16
    connection-minimum-idle-size: 8
    # 超时配置（毫秒）
    timeout: 3000
    connect-timeout: 3000
    retry-attempts: 3
    retry-interval: 1500
```

> 注意：`address` 必须带协议前缀 `redis://`，密码为 `redis://user:password@host:port`。

### 3.2 哨兵模式（生产环境高可用）

```yaml
redisson:
  sentinel-servers-config:
    master-name: mymaster
    sentinel-addresses:
      - redis://sentinel1:26379
      - redis://sentinel2:26379
      - redis://sentinel3:26379
    password: ${REDIS_PASSWORD:}
    database: 0
```

### 3.3 集群模式（生产环境大数据量）

```yaml
redisson:
  cluster-servers-config:
    node-addresses:
      - redis://node1:6379
      - redis://node2:6379
      - redis://node3:6379
      - redis://node4:6379
    password: ${REDIS_PASSWORD:}
    scan-interval: 5000
```

---

## 四、基础操作

### 4.1 注入 RedissonClient

```java
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedissonClient redissonClient;
}
```

### 4.2 String 操作 —— `RBucket`

```java
// 获取字符串对象（类似 Jedis 的 get/set）
RBucket<String> bucket = redissonClient.getBucket("user:name");

// 设置值
bucket.set("张三");

// 设置值并带过期时间
bucket.set("张三", 30, TimeUnit.MINUTES);

// 获取值
String name = bucket.get();

// 判断是否存在
boolean exists = bucket.isExists();

// 删除
bucket.delete();

// 原子性设置：如果不存在则设置（setnx）
boolean success = bucket.trySet("张三");

// 原子性设置：如果不存在则设置，带过期时间
boolean success = bucket.trySet("张三", 30, TimeUnit.MINUTES);

// 获取并设置新值（返回旧值）
String oldValue = bucket.getAndSet("李四");

// 存储对象（自动序列化）
RBucket<User> userBucket = redissonClient.getBucket("user:1");
userBucket.set(new User(1L, "张三", 20));
User user = userBucket.get();
```

### 4.3 Hash 操作 —— `RMap`

```java
RMap<String, String> userMap = redissonClient.getMap("user:info");

// 设置字段
userMap.put("name", "张三");
userMap.put("age", "20");
userMap.put("email", "zhangsan@example.com");

// 批量设置
Map<String, String> map = new HashMap<>();
map.put("name", "张三");
map.put("age", "20");
userMap.putAll(map);

// 获取单个字段
String name = userMap.get("name");

// 获取所有字段
Map<String, String> allFields = userMap.readAllMap();

// 判断字段是否存在
boolean hasName = userMap.containsKey("name");

// 删除字段
userMap.fastRemove("email");

// 带过期时间的 Map（Session 场景常用）
RMap<String, String> sessionMap = redissonClient.getMap("session:abc123");
sessionMap.put("userId", "1001");
sessionMap.expire(30, TimeUnit.MINUTES);

// 存储 Java 对象
RMap<Long, User> userObjMap = redissonClient.getMap("users");
userObjMap.put(1L, new User(1L, "张三", 20));
User user = userObjMap.get(1L);
```

### 4.4 List 操作 —— `RList`

```java
RList<String> list = redissonClient.getList("user:tags");

// 从尾部添加
list.add("Java");
list.add("Python");
list.add("Go");

// 批量添加
list.addAll(Arrays.asList("C++", "Rust"));

// 获取元素
String first = list.get(0);
int size = list.size();

// 获取所有元素
List<String> allTags = list.readAll();

// 从头部弹出（队列效果）
String tag = list.remove(0);

// 从尾部弹出（栈效果）
String last = list.remove(list.size() - 1);

// 判断是否包含
boolean hasJava = list.contains("Java");

// 设置固定大小（超出自动丢弃最旧元素，LRU 缓存）
list.add("Redis");
list.trim(0, 99);  // 只保留前 100 个
```

### 4.5 Set 操作 —— `RSet`

```java
RSet<String> set = redissonClient.getSet("user:roles");

// 添加元素
set.add("admin");
set.add("user");
set.add("editor");

// 批量添加
set.addAll(Arrays.asList("moderator", "vip"));

// 获取所有元素
Set<String> allRoles = set.readAll();

// 判断是否包含
boolean isAdmin = set.contains("admin");

// 删除元素
set.remove("editor");

// 获取集合大小
int size = set.size();

// 随机获取一个元素
String randomRole = set.random();

// 交集 / 并集 / 差集
RSet<String> set1 = redissonClient.getSet("set1");
RSet<String> set2 = redissonClient.getSet("set2");
Set<String> intersection = set1.readIntersection(Sets.of("set2"));
Set<String> union = set1.readUnion(Sets.of("set2"));
Set<String> diff = set1.readDiff(Sets.of("set2"));
```

### 4.6 ZSet 操作 —— `RScoredSortedSet`

```java
RScoredSortedSet<String> rankSet = redissonClient.getScoredSortedSet("user:ranking");

// 添加元素（元素, 分数）
rankSet.add(100, "user1");
rankSet.add(200, "user2");
rankSet.add(150, "user3");

// 获取元素分数
double score = rankSet.getScore("user1");

// 按分数从小到大获取
Collection<String> top3 = rankSet.valueRange(0, 2);

// 按分数从大到小获取
Collection<String> top3Desc = rankSet.valueRangeReversed(0, 2);

// 获取排名（从 0 开始）
int rank = rankSet.rank("user2");

// 增加分数（常用于排行榜累加）
rankSet.addScore("user1", 50);  // user1 的分数变为 150

// 删除元素
rankSet.remove("user3");

// 获取所有元素及其分数
Collection<ScoredEntry<String>> entries = rankSet.entryRange(0, -1);
for (ScoredEntry<String> entry : entries) {
    System.out.println(entry.getValue() + " : " + entry.getScore());
}
```

### 4.7 通用操作

```java
// 判断 key 是否存在
boolean exists = redissonClient.getBucket("some:key").isExists();

// 删除任意类型的 key
redissonClient.getKeys().delete("some:key");

// 批量删除
redissonClient.getKeys().delete("key1", "key2", "key3");

// 批量删除（模糊匹配）
redissonClient.getKeys().deleteByPattern("user:*");

// 设置过期时间
RBucket<String> bucket = redissonClient.getBucket("temp:key");
bucket.expire(60, TimeUnit.SECONDS);

// 获取剩余过期时间
long ttl = bucket.remainTimeToLive();  // 毫秒，-1 表示永不过期，-2 表示不存在

// 移除过期时间（持久化）
bucket.clearExpire();

// 模糊查询 key（慎用，生产大数据量时可能阻塞）
Iterable<String> keys = redissonClient.getKeys().getKeysByPattern("user:*");
Iterable<String> keysByPattern = redissonClient.getKeys().getKeysByPattern("order:*", 100);

// 遍历所有 key
for (String key : redissonClient.getKeys().getKeys()) {
    System.out.println(key);
}
```

---

## 五、高级应用

### 5.1 分布式锁 —— `RLock`

```java
// 获取锁对象
RLock lock = redissonClient.getLock("order:create:lock");

try {
    // 方式 1：阻塞等待获取锁（默认 30 秒自动释放）
    lock.lock();
    // 执行业务...

    // 方式 2：设置锁自动过期时间（避免死锁）
    lock.lock(10, TimeUnit.SECONDS);

    // 方式 3：非阻塞尝试获取锁
    boolean acquired = lock.tryLock();
    if (acquired) {
        // 获取到锁，执行业务...
    }

    // 方式 4：带超时 + 带等待时间（最常用）
    // waitTime=5秒：最多等待 5 秒获取锁
    // leaseTime=10秒：获取到锁后 10 秒自动释放
    boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
    if (acquired) {
        // 执行业务...
    } else {
        throw new RuntimeException("获取锁失败，请稍后重试");
    }

} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
} finally {
    // 释放锁（必须放在 finally 中）
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

### 5.2 公平锁 —— `RFairLock`

```java
RLock fairLock = redissonClient.getFairLock("fair:lock");

try {
    // 按请求顺序获取锁，避免饥饿
    fairLock.lock();
    // 业务逻辑...
} finally {
    if (fairLock.isHeldByCurrentThread()) {
        fairLock.unlock();
    }
}
```

### 5.3 读写锁 —— `RReadWriteLock`

```java
RReadWriteLock rwLock = redissonClient.getReadWriteLock("config:lock");

// 读锁（共享，多个线程可以同时读）
RLock readLock = rwLock.readLock();
readLock.lock(30, TimeUnit.SECONDS);
try {
    String config = readConfigFromDB();
    cache.put("config", config);
} finally {
    readLock.unlock();
}

// 写锁（独占，同时只能一个线程写）
RLock writeLock = rwLock.writeLock();
writeLock.lock(30, TimeUnit.SECONDS);
try {
    updateConfig(newConfig);
} finally {
    writeLock.unlock();
}
```

### 5.4 布隆过滤器 —— `RBloomFilter`

```java
// 初始化（创建时指定预计元素数量和误差率）
RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("email:blacklist");
bloomFilter.tryInit(1000000L, 0.01);  // 预计 100 万元素，误差率 1%

// 添加元素
bloomFilter.add("spam@example.com");
bloomFilter.add("ad@test.com");

// 判断是否存在
if (bloomFilter.contains("spam@example.com")) {
    // 大概率存在（可能有误判，但不会漏判）
    System.out.println("邮箱在黑名单中");
}

// 获取预估数量
long count = bloomFilter.count();
```

> 典型场景：缓存穿透防护、垃圾邮件过滤、爬虫去重。

### 5.5 限流器 —— `RRateLimiter`

```java
RRateLimiter rateLimiter = redissonClient.getRateLimiter("api:rate:limit");

// 初始化限流器
// 模式：OVERALL（全局） / PER_CLIENT（每个客户端独立）
// 速率：每 1 秒最多 10 个令牌
rateLimiter.trySetRate(RateType.OVERALL, 10, 1, RateIntervalUnit.SECONDS);

// 尝试获取令牌
if (rateLimiter.tryAcquire(1)) {
    // 获取到令牌，执行请求
    handleRequest();
} else {
    // 限流，拒绝请求
    throw new RuntimeException("请求过于频繁，请稍后再试");
}

// 尝试获取多个令牌，带等待时间
if (rateLimiter.tryAcquire(5, 3, TimeUnit.SECONDS)) {
    // 最多等待 3 秒获取 5 个令牌
    handleRequest();
}
```

### 5.6 计数器 —— `RAtomicLong` / `RAtomicDouble`

```java
// 原子计数器
RAtomicLong counter = redissonClient.getAtomicLong("page:view:count");

// 递增
long newValue = counter.incrementAndGet();
long oldValue = counter.getAndIncrement();

// 递减
counter.decrementAndGet();

// 增加指定值
counter.addAndGet(10);

// 获取当前值
long count = counter.get();

// 设置初始值
counter.set(0);
```

### 5.7 延迟队列 —— `RDelayedQueue`

```java
// 底层队列
RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue("order:timeout");
// 延迟队列
RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);

// 放入延迟队列（30 秒后进入底层队列）
delayedQueue.offer("order:12345", 30, TimeUnit.SECONDS);

// 在另一个线程/服务中消费底层队列（会阻塞直到有元素）
String orderNo = blockingQueue.take();  // 30 秒后才能拿到
processTimeoutOrder(orderNo);

// 销毁延迟队列（释放资源）
delayedQueue.destroy();
```

> 典型场景：订单超时自动取消、定时任务、延迟通知。

### 5.8 发布/订阅 —— `RTopic`

```java
// 发布者
RTopic topic = redissonClient.getTopic("order:notification");
long receivers = topic.publish(new OrderEvent("order:123", "CREATED"));

// 订阅者（通常在不同服务/线程中）
RTopic topic = redissonClient.getTopic("order:notification");
topic.addListener(OrderEvent.class, (channel, msg) -> {
    System.out.println("收到消息: " + msg);
    handleOrderEvent(msg);
});

// 模式订阅（通配符）
RPatternTopic patternTopic = redissonClient.getPatternTopic("order:*");
patternTopic.addListener(String.class, (pattern, channel, msg) -> {
    System.out.println("匹配模式: " + pattern + ", 频道: " + channel);
});
```

### 5.9 位图操作 —— `RBitSet`

```java
RBitSet bitSet = redissonClient.getBitSet("user:login:202604");

// 设置位
bitSet.set(1);   // 1 号登录
bitSet.set(15);  // 15 号登录

// 获取位
boolean day1Login = bitSet.get(1);   // true
boolean day2Login = bitSet.get(2);   // false

// 统计登录天数
int loginCount = bitSet.cardinality();

// 与另一个 BitMap 做运算
RBitSet bitSet2 = redissonClient.getBitSet("user:login:202603");
bitSet.and(bitSet2);  // 两个月都登录的日期
bitSet.or(bitSet2);   // 任一月份登录的日期
```

> 典型场景：用户签到、活跃统计。

### 5.10 HyperLogLog —— `RHyperLogLog`

```java
RHyperLogLog<String> hll = redissonClient.getHyperLogLog("uv:20260415");

// 添加元素
hll.add("user1");
hll.add("user2");
hll.add("user1");  // 重复添加不计入

// 添加多个
hll.addAll(Arrays.asList("user3", "user4", "user5"));

// 获取基数（去重后的数量）
long uv = hll.count();
```

> 典型场景：UV 统计、海量数据去重。内存占用极小（12KB 可存储近 2^64 个元素）。

---

## 六、Redis + 缓存实战

### 6.1 缓存基本模式（Cache Aside）

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RedissonClient redissonClient;

    public User getUserById(Long id) {
        String key = "user:" + id;
        RBucket<User> bucket = redissonClient.getBucket(key);

        // 1. 先查缓存
        User user = bucket.get();
        if (user != null) {
            return user;
        }

        // 2. 缓存未命中，查数据库
        user = userMapper.selectById(id);
        if (user != null) {
            // 3. 写入缓存，设置过期时间
            bucket.set(user, 30, TimeUnit.MINUTES);
        }
        return user;
    }

    public void updateUser(User user) {
        // 1. 更新数据库
        userMapper.updateById(user);
        // 2. 删除缓存（而非更新，避免并发问题）
        String key = "user:" + user.getId();
        redissonClient.getBucket(key).delete();
    }

    public void deleteUser(Long id) {
        userMapper.deleteById(id);
        redissonClient.getBucket("user:" + id).delete();
    }
}
```

### 6.2 缓存穿透防护

```java
public User getUserById(Long id) {
    String key = "user:" + id;
    RBucket<User> bucket = redissonClient.getBucket(key);

    User user = bucket.get();
    if (user != null) {
        // 注意：空值对象需要特殊判断
        if (user.getIsNull()) {
            return null;  // 数据库中也不存在
        }
        return user;
    }

    // 查数据库
    user = userMapper.selectById(id);
    if (user != null) {
        bucket.set(user, 30, TimeUnit.MINUTES);
    } else {
        // 缓存空值，防止穿透（设置较短过期时间）
        bucket.set(new User().setIsNull(true), 5, TimeUnit.MINUTES);
    }
    return user;
}
```

> 更好的方案是用布隆过滤器（见 5.4 节），在请求到达 DB 之前判断 key 是否存在。

### 6.3 分布式锁 + 缓存双写一致性

```java
public void updateUser(User user) {
    RLock lock = redissonClient.getLock("user:update:" + user.getId());
    try {
        lock.lock(10, TimeUnit.SECONDS);
        // 获取锁后双重检查
        String key = "user:" + user.getId();
        redissonClient.getBucket(key).delete();
        userMapper.updateById(user);
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

### 6.4 缓存预热（项目启动时）

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmUp implements ApplicationRunner {

    private final UserMapper userMapper;
    private final RedissonClient redissonClient;

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始缓存预热...");
        List<User> hotUsers = userMapper.selectHotUsers(100);

        for (User user : hotUsers) {
            String key = "user:" + user.getId();
            RBucket<User> bucket = redissonClient.getBucket(key);
            bucket.set(user, 1, TimeUnit.HOURS);
        }
        log.info("缓存预热完成，共加载 {} 条数据", hotUsers.size());
    }
}
```

---

## 七、Lua 脚本执行

```java
// 方式 1：直接执行 Lua
RScript script = redissonClient.getScript();

// 原子性递增并设置上限
Long result = script.eval(
    RScript.Mode.READ_WRITE,
    "local current = redis.call('GET', KEYS[1]); " +
    "if current == nil or tonumber(current) < tonumber(ARGV[2]) then " +
    "  return redis.call('INCR', KEYS[1]); " +
    "else " +
    "  return tonumber(current); " +
    "end",
    RScript.ReturnType.INTEGER,
    Arrays.asList("rate:limit:user:1"),  // KEYS
    "rate:limit:user:1", "100"           // ARGV
);

// 方式 2：预先加载脚本（SHA 缓存，更高效）
RScript script = redissonClient.getScript();
script.getScript(ScriptCodec, "lua script content");
```

---

## 八、监控与调优

### 8.1 Redisson 配置调优

```yaml
redisson:
  single-server-config:
    connection-pool-size: 32           # 连接池大小（根据并发量调整）
    connection-minimum-idle-size: 16   # 最小空闲连接
    idle-connection-timeout: 60000     # 空闲连接超时（毫秒）
    timeout: 3000                      # 命令等待超时
    connect-timeout: 3000              # 连接超时
    retry-attempts: 3                  # 重试次数
    retry-interval: 1500               # 重试间隔
    subscriptions-per-connection: 5    # 每个连接的订阅数
```

### 8.2 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| `RedisConnectionException` | Redis 服务不可达 | 检查网络、密码、端口 |
| `RedisResponseException: ERR unknown command` | 命令被禁用（如云 Redis） | 联系云服务商开放 |
| 锁超时业务未完成 | `leaseTime` 设置过短 | 加大时间或使用看门狗 |
| 内存溢出 | key 过期策略不当 | 合理设置 `expire` |
| `KEYS *` 阻塞 | 生产大量 key 时模糊查询 | 改用 `getKeysByPattern` + limit |

---

## 九、完整配置类示例

如果需要 Java Config 方式创建 `RedissonClient`：

```java
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;

        config.useSingleServer()
                .setAddress(address)
                .setPassword(StringUtils.hasText(password) ? password : null)
                .setDatabase(database)
                .setConnectionPoolSize(32)
                .setConnectionMinimumIdleSize(16)
                .setTimeout(3000)
                .setConnectTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        return Redisson.create(config);
    }
}
```

> 使用 `redisson-spring-boot-starter` 时，上面的配置类**不需要**，starter 会自动配置。
> 只在需要精细控制时才用 Java Config。

---

## 十、总结速查表

| 需求 | Redisson API | 说明 |
|------|-------------|------|
| 简单键值对 | `RBucket` | 字符串/对象 |
| Hash | `RMap` | 对象属性存储 |
| 列表 | `RList` | 队列/栈 |
| 去重集合 | `RSet` | 标签/角色 |
| 有序集合 | `RScoredSortedSet` | 排行榜 |
| 分布式锁 | `RLock` | 互斥同步 |
| 公平锁 | `RFairLock` | 排队获取 |
| 读写锁 | `RReadWriteLock` | 读多写少场景 |
| 布隆过滤器 | `RBloomFilter` | 穿透防护 |
| 限流 | `RRateLimiter` | 令牌桶 |
| 计数器 | `RAtomicLong` | 累加/累减 |
| 延迟队列 | `RDelayedQueue` | 订单超时 |
| 发布订阅 | `RTopic` | 消息通知 |
| 位图 | `RBitSet` | 签到统计 |
| UV 统计 | `RHyperLogLog` | 去重计数 |
