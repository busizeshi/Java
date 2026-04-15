# 第1课：pom.xml 文件解析

> 本文档逐段讲解本项目 `pom.xml` 的结构和含义，帮助 Java 初学者理解 Maven 项目配置。

---

## 一、项目基本信息（GAV 坐标）

```xml
<groupId>com.jwd</groupId>
<artifactId>java-ai</artifactId>
<version>1.0-SNAPSHOT</version>
<packaging>jar</packaging>
```

| 字段 | 值 | 说明 |
|------|------|------|
| `groupId` | `com.jwd` | 组织/公司标识，通常用反向域名 |
| `artifactId` | `java-ai` | 项目名称（模块名） |
| `version` | `1.0-SNAPSHOT` | 版本号，`SNAPSHOT` 表示开发中版本 |
| `packaging` | `jar` | 打包方式，Spring Boot 默认打成可执行 jar |

> **类比理解**：GAV 坐标就像项目的"身份证号"，Maven 仓库中靠这三者唯一定位一个构件。

---

## 二、父 POM（Parent）

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.4</version>
    <relativePath/>
</parent>
```

**为什么要继承 `spring-boot-starter-parent`？**

1. **自动管理依赖版本** — 父 POM 中已经定义了大量常用依赖的兼容版本，子项目无需手动指定版本号
2. **默认插件配置** — 自动配置了 `maven-compiler-plugin`、`spring-boot-maven-plugin` 等
3. **默认属性** — 如编码默认 UTF-8、资源过滤等

> `<relativePath/>` 表示不从本地路径查找父 POM，直接从 Maven 远程仓库下载。

---

## 三、Properties（属性配置）

```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>${java.version}</maven.compiler.source>
    <maven.compiler.target>${java.version}</maven.compiler.target>
    <spring-ai.version>1.0.0</spring-ai.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

| 属性 | 说明 |
|------|------|
| `java.version=21` | 使用 Java 21（LTS 长期支持版本），支持虚拟线程、Record、Pattern Matching 等新特性 |
| `spring-ai.version=1.0.0` | 预先声明了 Spring AI 的版本号，后续引入 Spring AI 相关依赖时无需再写版本 |
| `sourceEncoding=UTF-8` | 源代码文件编码 |

> **为什么选 Java 21？**
> Java 21 是当前最新的 LTS 版本，Spring Boot 3.x 要求最低 Java 17。选择 21 可以享受更多语言新特性。

---

## 四、Dependency Management（依赖版本管理）

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 什么是 BOM？

**BOM（Bill of Materials）** 是一张"依赖版本清单"。通过 `<scope>import</scope>` 导入后，BOM 中声明的所有依赖版本都会被自动管理。

> **类比**：BOM 就像一份"菜单价目表"，你只需要点菜（声明依赖 groupId + artifactId），价格（版本号）已经由 BOM 定好了。

**为什么要引入 Spring AI BOM？**

Spring AI 包含多个模块（openai、ollama、vector-store 等），它们之间需要版本一致。BOM 确保了所有 Spring AI 子模块使用相同版本，避免版本冲突。

---

## 五、Dependencies（依赖声明）

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependencies>
</dependencies>
```

### 什么是 Starter？

Spring Boot Starter 是一组**预打包的依赖集合**，让你"开箱即用"。

`spring-boot-starter-web` 自动引入了：

| 依赖 | 作用 |
|------|------|
| Spring MVC | Web 框架核心 |
| Tomcat（嵌入式） | 内嵌 Servlet 容器，无需部署到外部 Tomcat |
| Jackson | JSON 序列化/反序列化 |
| Spring Boot AutoConfiguration | 自动配置 |

> **注意**：这里没有写 `<version>`，因为父 POM `spring-boot-starter-parent` 已经管理了版本。

### 当前依赖状态

目前项目**仅引入了 Web 模块**，后续学习 Spring AI 时还需要添加：

```xml
<!-- 未来会添加的依赖示例 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

---

## 六、Build / Plugins（构建插件）

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-enforcer-plugin</artifactId>
            <executions>
                <execution>
                    <id>enforce-versions</id>
                    <goals>
                        <goal>enforce</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <requireMavenVersion>
                                <version>3.9</version>
                            </requireMavenVersion>
                            <requireJavaVersion>
                                <version>21</version>
                            </requireJavaVersion>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### maven-enforcer-plugin 的作用

这是一个**环境版本检查插件**，在构建前强制验证：

- Maven 版本 >= 3.9
- Java 版本 >= 21

如果环境版本不满足要求，构建会直接失败并提示错误。这确保了所有开发者使用一致的构建环境，避免"在我机器上是好的"这类问题。

---

## 七、整体结构总结

```
java-ai (pom.xml)
│
├── 继承 → spring-boot-starter-parent (3.4.4)
│           └── 自动管理 Spring Boot 生态依赖版本
│
├── 导入 BOM → spring-ai-bom (1.0.0)
│               └── 统一管理 Spring AI 模块版本
│
├── 当前依赖 → spring-boot-starter-web
│               └── Web 开发基础能力（Spring MVC + 内嵌 Tomcat + JSON）
│
└── 构建插件 → maven-enforcer-plugin
                └── 强制 Maven >= 3.9、Java >= 21
```

---

## 八、待办（Next Steps）

- [ ] 添加 Spring AI 相关 Starter 依赖
- [ ] 配置 AI 服务提供商（OpenAI / Ollama / 通义千问 等）
- [ ] 编写第一个 AI 对话接口
