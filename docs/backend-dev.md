# 后端项目开发规范 Skill

## Skill 概述

本 Skill 定义了基于 Spring Boot 的后端项目开发规范，采用 **DDD（领域驱动设计）** 与**充血模型**，确保代码风格统一、接口格式一致、分层架构清晰。适用于所有新项目的开发。

## 技术栈与版本规范

### Java 版本
- **Java 版本**：Java 21（LTS）
- **编译目标**：Java 21
- **编码格式**：UTF-8

### Spring Boot 版本
- **Spring Boot**：3.4.x（与 Java 21 配套）
- **Spring Framework**：6.2.x

### 核心依赖版本
| 依赖 | 版本 |
|------|------|
| Lombok | latest (通过 Spring Boot 管理) |
| Swagger (SpringDoc) | 2.x（`springdoc-openapi-starter-webmvc-ui`） |
| ORM | **MyBatis-Plus**（Spring Boot 3 使用 `mybatis-plus-spring-boot3-starter`） |
| 数据库 | **GaussDB 8.2**（持久化与查询目标库） |
| JDBC 驱动 | **PostgreSQL JDBC Driver**（`org.postgresql:postgresql`；适配 GaussDB（openGauss 内核）PostgreSQL 协议） |
| Jackson | 由 Spring Boot 管理（2.15+） |
| Guava | 由项目统一指定或与 Boot 兼容版本 |

### 数据库规范（华为高斯 GaussDB 8.2）

- 数据库使用**华为高斯 GaussDB 8.2（openGauss 内核）**，对外采用 **PostgreSQL 协议**。
- 采用 **MyBatis-Plus** 操作数据，**禁止编写复杂原生 SQL**（简单、可维护的短 SQL 需在评审中说明必要性）。
- 分页统一使用 **`LIMIT offset, size`** 标准语法（与 MyBatis-Plus 分页插件配置一致）。
- 字符集、排序规则等以数据库实例配置为准（建议 UTF-8 / UTF8MB4 等团队统一标准）。
- **禁止**使用存储过程、触发器、自定义函数、外键、事件调度器。
- 字段类型优先使用 **PostgreSQL/openGauss 通用类型**，避免高斯私有语法。

### 新增代码规模与复杂度门禁

以下指标针对**新增**函数与文件，建议在 CI 或本地静态检查中落实：

| 项 | 上限 | 说明 |
|----|------|------|
| 圈复杂度 | ≤ 20 | 单函数（含分支、循环、逻辑与/或等） |
| 单函数代码行数 | ≤ 50 | 统计非空非注释行 |
| 单文件代码行数 | ≤ 2000 | 过大文件应拆分 |

### Java 21 特性使用规范

#### 1. Lambda 表达式
```java
// 推荐：使用 Lambda 简化集合操作
List<String> result = list.stream()
    .filter(item -> item != null)
    .map(String::toUpperCase)
    .collect(Collectors.toList());

// 遍历集合
list.forEach(item -> {
    // 处理逻辑
});
```

#### 2. Stream API
```java
// 过滤和转换
List<ExampleDto> result = dtoList.stream()
    .filter(dto -> dto.getStatus() == 1)
    .map(dto -> convertToVo(dto))
    .collect(Collectors.toList());

// 查找第一个
Optional<ExampleDto> first = dtoList.stream()
    .filter(dto -> dto.getId().equals(id))
    .findFirst();

// 统计
long count = dtoList.stream()
    .filter(dto -> dto.getStatus() == 1)
    .count();
```

#### 3. Optional 类
```java
// 推荐使用 Optional 处理可能为空的值
public ResponseObject<ExampleDto> queryById(String id) {
    ExampleDto dto = mapper.queryById(id);
    if (dto == null) {
        return new ResponseObject<ExampleDto>().failure("数据不存在");
    }
    return new ResponseObject<ExampleDto>().success(dto);
}

// Optional 链式调用
Optional.ofNullable(dto)
    .map(ExampleDto::getName)
    .ifPresent(name -> log.info("Name: {}", name));
```

#### 4. 日期时间 API
```java
// 使用 java.time 处理日期与时间（推荐）
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 当前日期时间
LocalDateTime now = LocalDateTime.now();

// 日期格式化
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
String formattedDate = now.format(formatter);

// 日期解析
LocalDate date = LocalDate.parse("2024-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
```

#### 5. 集合与 Stream 收尾（Java 16+）
```java
// Stream 收集为 List（不可变列表，Java 16+）
List<String> upper = list.stream()
    .map(String::toUpperCase)
    .toList();

// 创建不可变集合（Java 9+ 工厂方法，优先于仅 Arrays.asList）
List<String> items = List.of("A", "B", "C");
```

#### 6. 可选的现代语法（按需使用，保持可读性优先）

- **`var`**：右侧类型明确时使用，避免滥用导致可读性下降。
- **`record`**：适合不可变 DTO、值对象，减少样板代码。
- **文本块 `"""`**：多行字符串、SQL/JSON 片段。
- **模式匹配**（`instanceof`、`switch`）：替代冗长 `if-else` + 强转。

```java
// 示例：模式匹配减少强转
if (e instanceof IllegalArgumentException iae) {
    log.warn("参数错误: {}", iae.getMessage());
}

// 示例：record 作为轻量 DTO
public record PageQuery(int pageNum, int pageSize) {
    public PageQuery {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
    }
}
```

### Java 21 生态与注意事项

1. **Jakarta EE 命名空间（Spring Boot 3）**：
   - 使用 `jakarta.annotation.Resource`、`jakarta.servlet.*`、`jakarta.validation.*` 等，**不再使用** `javax.*` 中与 EE 迁移相关的包。

2. **Stream 使用注意**：
   - 并行流需谨慎使用，确保线程安全
   - Stream 只能消费一次，不能重复使用

3. **预览/孵化器特性**：若启用 JDK 预览特性，须在团队内统一并在构建中显式开启，文档中单独说明。
```

## DDD 领域驱动设计与充血模型

本项目**必须**采用 **DDD（领域驱动设计）** 进行业务分析与建模，并在实现上采用**充血模型**，禁止以「只有数据、没有行为」的贫血模型作为领域核心。

### 总体要求

| 要求 | 说明 |
|------|------|
| **领域优先** | 业务规则、不变量、状态流转优先落在 **领域层（`domain`）**，而非全部堆在 Application Service 或 Mapper SQL 中。 |
| **充血模型** | **实体（Entity）、值对象（Value Object）、聚合根（Aggregate Root）** 应封装与其身份或值语义相关的**行为与方法**（校验、状态变更、领域计算等）；避免仅暴露 getter/setter、由外层到处 `if-else` 操纵字段。 |
| **应用层职责** | `application/service` 负责**用例编排、事务边界、与基础设施交互**，应**薄**；复杂业务逻辑委托给领域对象或领域服务（Domain Service）。 |
| **基础设施层** | 负责技术实现：MyBatis-Plus Mapper、外部 API、消息等；通过**仓储接口（Repository 接口在领域层定义，实现在基础设施层）** 持久化聚合，避免领域层依赖具体 ORM 注解或 SQL。 |

### 包与概念建议（可在 `domain` 下按业务模块划分）

- **聚合 / 实体 / 值对象**：承载业务行为与不变量；对外暴露表达领域意图的方法（如 `order.submit()`、`order.cancel(reason)`），而非仅 `setStatus`。
- **领域服务（Domain Service）**：当某行为不适合放在单一实体上时，以无状态或显式依赖协作对象的方式实现。
- **仓储接口**：在 `domain`（或 `domain.repository`）声明，命名体现聚合粒度（如 `OrderRepository`）；**禁止**在领域层依赖 MyBatis-Plus Mapper 注解或 SQL。
- **领域事件（可选）**：跨聚合协作时可采用领域事件，发布与订阅边界在文档与代码评审中保持一致。

### 与贫血模型的区别（反例）

- ❌ `Order` 仅有一堆字段与 getter/setter，所有提交、取消、金额计算均在 `OrderService` 中通过 `order.setXxx` 完成。
- ✅ `Order` 聚合内提供 `submit()`、`cancel(CancelReason)` 等方法，内部校验状态与不变量；应用服务调用领域方法并调用仓储保存。

### 实施注意

- 新功能评审须能说明：**聚合边界、实体与值对象划分、行为归属**。
- 若与既有贫血代码共存，**新增代码优先遵守 DDD + 充血模型**；遗留模块可渐进重构并在任务单中跟踪。

## 一、接口返回对象规范

### 1.1 ResponseObject 标准结构

所有业务接口（除特殊情况外）必须使用 `ResponseObject<T>` 作为统一返回对象。

**ResponseObject 结构：**

不再使用独立的 `Meta` 对象；**`success`（是否成功）**与 **`message`（提示信息）** 直接作为 `ResponseObject` 的字段，**不再使用 `number`（返回数量）** 字段。

```java
public class ResponseObject<T> {
    private boolean success;   // 是否成功
    private String message;    // 提示信息
    private T data;            // 具体数据（业务载荷）

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    // 链式调用方法
    public ResponseObject<T> setDataAndReturn(T data) {
        this.data = data;
        return this;
    }

    // 成功响应方法
    public ResponseObject<T> success() {
        this.success = true;
        this.message = CommonConst.OK;
        return this;
    }

    /** 成功并返回数据（列表长度等由客户端取 data 自行计算，不再单独返回 number） */
    public ResponseObject<T> success(T data) {
        this.success = true;
        this.message = CommonConst.OK;
        this.data = data;
        return this;
    }

    /** 成功且仅返回提示文案（无业务 data 或 data 为空场景） */
    public ResponseObject<T> success(String message) {
        this.success = true;
        this.message = message;
        return this;
    }

    // 失败响应方法
    public ResponseObject<T> failure() {
        this.success = false;
        this.message = CommonConst.ERROR;
        return this;
    }

    public ResponseObject<T> failure(String message) {
        this.success = false;
        this.message = message;
        return this;
    }
}
```

### 1.2 常用常量

```java
public class CommonConst {
    public static final String OK = "OK";           // 成功状态
    public static final String ERROR = "ERROR";     // 失败状态
}
```

### 1.3 ResponseObject 使用示例

#### Service 层返回示例：

```java
@Service
@Slf4j
public class ExampleService {

    /**
     * 查询列表数据
     */
    public ResponseObject<List<String>> queryList(String param) {
        List<String> result = mapper.queryList(param);
        return new ResponseObject<List<String>>().success(result);
    }

    /**
     * 执行操作
     */
    public ResponseObject<String> executeOperation(String param) {
        try {
            // 业务逻辑
            return new ResponseObject<String>().success("操作成功");
        } catch (Exception e) {
            log.error("操作失败", e);
            return new ResponseObject<String>().failure("操作失败：" + e.getMessage());
        }
    }

    /**
     * 查询单个对象
     */
    public ResponseObject<ExampleDto> queryOne(String id) {
        ExampleDto result = mapper.queryOne(id);
        if (result == null) {
            return new ResponseObject<ExampleDto>().failure("数据不存在");
        }
        return new ResponseObject<ExampleDto>().success(result);
    }
}
```

#### Controller 层返回示例：

```java
@RestController
@RequestMapping("/api/v1/examples")
@Tag(name = "ExampleController", description = "示例接口层")
public class ExampleController {

    @Resource
    private ExampleService exampleService;

    /**
     * 查询示例集合（资源名用复数名词，见「RESTful 接口约定」）
     */
    @Operation(summary = "查询示例数据集合")
    @GetMapping(produces = "application/json; charset=utf-8")
    public ResponseObject<List<String>> queryCollection(
        @RequestParam(value = "keyword", required = false) String keyword) {
        return exampleService.queryList(keyword);
    }

    /**
     * 提交命令类操作（以子资源 commands 表达，避免在路径中使用动词）
     */
    @Operation(summary = "提交示例命令")
    @PostMapping(value = "/commands", produces = "application/json; charset=utf-8")
    public ResponseObject<String> submitCommand(@RequestBody ExampleRequest request) {
        return exampleService.executeOperation(request.getParam());
    }
}
```

### 1.4 返回类型选择规范

| 场景 | 返回类型 | 说明 |
|------|---------|------|
| 普通业务接口（查询/操作） | `ResponseObject<T>` | 标准返回格式 |
| 文件下载/导出 | `ResponseEntity<byte[]>` | 二进制数据 |
| 第三方代理接口 | `ResponseEntity<T>` | 原样返回 |
| 特殊场景（如登录） | 直接返回对象 | 需要特殊说明 |

## 二、Controller 层开发规范

### 2.1 类级别注解规范

```java
@RestController                          // 必须使用 RestController
@RequestMapping("/api/v1/order-items")   // 类级路径：名词、复数资源、小写；可用 /api/v1 作为版本前缀
@Tag(name = "ControllerName", description = "接口描述")  // Swagger 文档注解
public class ExampleController {
    // ...
}
```

### 2.2 方法级别注解规范

```java
/**
 * 方法功能描述
 *
 * @param param1 参数1说明
 * @param param2 参数2说明
 * @return 返回值说明
 */
@Operation(summary = "接口简要描述")
@GetMapping(produces = "application/json; charset=utf-8")
public ResponseObject<ReturnType> queryCollection(
    @RequestParam(value = "param1", required = false) String param1,
    @RequestParam(value = "param2", required = true) String param2) {
    return service.queryCollection(param1, param2);
}
```

### 2.3 RESTful 接口约定

设计与评审接口时须符合 **REST** 风格（资源导向、HTTP 语义化），与统一返回体 `ResponseObject` 不冲突。

| 要求 | 说明 |
|------|------|
| **资源用名词** | URL 表示**资源**（人、事、物、集合），**避免**在路径中使用 `create`、`update`、`delete`、`list`、`execute` 等动词；操作用 **HTTP 方法**表达。 |
| **集合用复数** | 集合资源名使用**复数英文名词**，如 `/orders`、`/users`、`/examples`；单条资源用路径变量：`/orders/{orderId}`。 |
| **层次清晰** | 子资源通过路径嵌套表达关系，如 `/users/{userId}/roles`；不宜过长时可用查询参数过滤。 |
| **版本与前缀** | 对外 API 建议使用统一前缀，如 `/api/v1/...`，避免与静态资源冲突。 |
| **语义化方法** | `GET` 安全且幂等（查询）；`POST` 新建或非幂等处理；`PUT` 全量替换；`PATCH` 部分更新；`DELETE` 删除。 |
| **状态码** | 在统一封装之外，仍应合理设置 HTTP 状态（如 400/404/409），与全局异常处理配合。 |

**路径示例（名词化）：**

| 非 REST 风格（避免） | 推荐 |
|---------------------|------|
| `GET /listOrders` | `GET /orders` |
| `POST /createOrder` | `POST /orders` |
| `POST /updateOrder` | `PUT /orders/{id}` 或 `PATCH /orders/{id}` |
| `POST /deleteOrder` | `DELETE /orders/{id}` |
| `GET /getUserById` | `GET /users/{id}` |

### 2.4 HTTP 方法选择规范

| 操作类型 | HTTP 方法 | 典型路径示例 |
|---------|----------|----------------|
| 查询集合 | GET | `GET /resources`（过滤条件用 Query 参数） |
| 查询单个 | GET | `GET /resources/{id}` |
| 创建 | POST | `POST /resources` |
| 全量更新 | PUT | `PUT /resources/{id}` |
| 部分更新 | PATCH | `PATCH /resources/{id}` |
| 删除 | DELETE | `DELETE /resources/{id}` |

复杂动作若无法映射为 CRUD，可用**名词化子资源**表达，如 `POST /resources/{id}/commands` 或团队约定的 `POST /resource-operations`（仍为名词短语），避免单层路径上出现动词。

### 2.5 参数注解规范

#### Query 参数（URL 参数）：

Query 中**使用名词或约定过滤字段名**，不出现动词；分页、排序等用通用参数名（如 `page`、`size`、`sort`）。

```java
@GetMapping(produces = "application/json; charset=utf-8")
public ResponseObject<Result> queryOrderItems(
    @RequestParam(value = "orderId", required = false) String orderId,
    @RequestParam(value = "name", required = true) String name,
    @RequestParam(value = "page", defaultValue = "1") int page) {
    // ...
}
```

#### Body 参数（请求体）：

```java
@PostMapping(consumes = "application/json", produces = "application/json; charset=utf-8")
public ResponseObject<Result> createOrderItem(@RequestBody ExampleRequest request) {
    // ...
}
```

#### Path 参数（路径变量）：

```java
@GetMapping(value = "/{id}", produces = "application/json; charset=utf-8")
public ResponseObject<Result> getById(@PathVariable("id") String id) {
    // ...
}
```

### 2.6 依赖注入规范

```java
@Resource  // 使用 jakarta.annotation.Resource 注入，不使用 @Autowired
private ExampleService exampleService;
```

### 2.7 Controller 完整示例

```java
/*
 * Copyright (c) Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.coretool.ui.controller;

import com.coretool.application.service.example.ExampleService;
import com.coretool.infrastructure.dto.ExampleDto;
import com.coretool.ui.response.ResponseObject;
import com.coretool.ui.vo.ExampleRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import jakarta.annotation.Resource;

/**
 * 示例接口层（资源名：examples）
 *
 * @author authorName
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/v1/examples")
@Tag(name = "ExampleController", description = "示例接口层")
public class ExampleController {

    @Resource
    private ExampleService exampleService;

    /**
     * 查询示例数据集合
     *
     * @param name 名称
     * @param type 类型
     * @return 示例数据列表
     */
    @Operation(summary = "查询示例数据集合")
    @GetMapping(produces = "application/json; charset=utf-8")
    public ResponseObject<List<ExampleDto>> queryCollection(
        @RequestParam(value = "name", required = false) String name,
        @RequestParam(value = "type", required = false) String type) {
        return exampleService.queryList(name, type);
    }

    /**
     * 创建示例数据
     *
     * @param request 请求参数
     * @return 操作结果
     */
    @Operation(summary = "创建示例数据")
    @PostMapping(consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<String> create(@RequestBody ExampleRequest request) {
        return exampleService.create(request);
    }

    /**
     * 全量更新指定示例
     *
     * @param id 资源标识
     * @param request 请求参数
     * @return 操作结果
     */
    @Operation(summary = "更新示例数据")
    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<String> replace(
        @PathVariable("id") String id,
        @RequestBody ExampleRequest request) {
        return exampleService.update(id, request);
    }

    /**
     * 删除指定示例
     *
     * @param id 资源标识
     * @return 操作结果
     */
    @Operation(summary = "删除示例数据")
    @DeleteMapping(value = "/{id}", produces = "application/json; charset=utf-8")
    public ResponseObject<String> remove(@PathVariable("id") String id) {
        return exampleService.delete(id);
    }
}
```

## 三、Service 层开发规范

### 3.1 类级别注解规范

```java
@Service
@Slf4j
public class ExampleService {
    // ...
}
```

### 3.2 方法开发规范

#### 查询方法：

```java
/**
 * 查询数据列表
 *
 * @param param1 参数1
 * @param param2 参数2
 * @return 数据列表
 */
public ResponseObject<List<ExampleDto>> queryList(String param1, String param2) {
    List<ExampleDto> result = exampleMapper.queryList(param1, param2);
    return new ResponseObject<List<ExampleDto>>().success(result);
}
```

#### 创建/更新方法：

```java
/**
 * 创建数据
 *
 * @param request 请求参数
 * @return 操作结果
 */
public ResponseObject<String> create(ExampleRequest request) {
    try {
        // 参数校验
        if (StringUtils.isEmpty(request.getName())) {
            return new ResponseObject<String>().failure("名称不能为空");
        }

        // 业务逻辑处理
        ExampleDto dto = new ExampleDto();
        BeanUtils.copyProperties(request, dto);

        // 写入 GaussDB 8.2（经 MyBatis-Plus Mapper）
        int count = exampleMapper.insert(dto);
        if (count > 0) {
            return new ResponseObject<String>().success("创建成功");
        } else {
            return new ResponseObject<String>().failure("创建失败");
        }
    } catch (Exception e) {
        log.error("创建数据失败", e);
        return new ResponseObject<String>().failure("创建失败：" + e.getMessage());
    }
}
```

### 3.3 依赖注入规范

```java
@Resource
private ExampleMapper exampleMapper;

@Resource
private OtherService otherService;
```

### 3.4 日志规范

```java
@Service
@Slf4j
public class ExampleService {

    public ResponseObject<String> method(String param) {
        // 正常日志
        log.info("执行方法，参数：{}", param);

        // 警告日志
        log.warn("数据为空，参数：{}", param);

        // 错误日志
        try {
            // 业务逻辑
        } catch (Exception e) {
            log.error("操作失败，参数：{}", param, e);
            return new ResponseObject<String>().failure("操作失败");
        }

        return new ResponseObject<String>().success();
    }
}
```

## 四、VO（View Object）规范

### 4.1 VO 类定义规范

```java
/*
 * Copyright (c) Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.coretool.ui.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 示例请求对象
 *
 * @author authorName
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExampleRequest {
    private String name;
    private String type;
    private Integer age;
}
```

### 4.2 使用 Lombok 注解

```java
@Data           // 自动生成 getter/setter/toString/equals/hashCode
@NoArgsConstructor // 无参构造
@AllArgsConstructor // 全参构造
public class ExampleRequest {
    // ...
}
```

## 五、异常处理规范

### 5.1 全局异常处理器

```java
/*
 * Copyright (c) Technologies Co., Ltd. 2015-2019. All rights reserved.
 */

package com.coretool.ui.controller.configure;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 全局异常统一处理
 *
 * @since 2019-12-31
 */
@ControllerAdvice
public class ExceptionHandlerController {
    private static final Log LOG = LogFactory.getLog(ExceptionHandlerController.class);

    /**
     * 系统运行时异常处理（未捕获的异常触发）
     *
     * @param req 请求参数
     * @param e   异常
     * @return ResponseEntity
     * @throws Exception 异常
     */
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ResponseEntity<Object> defaultErrorHandler(HttpServletRequest req, Exception e) throws Exception {
        LOG.error("Internal Error: ", e);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "内部错误！麻烦通过右上角的'反馈意见'功能反馈，谢谢！";

        // 根据具体异常类型细化错误信息
        if (e instanceof MissingServletRequestParameterException || e instanceof MethodArgumentTypeMismatchException) {
            status = HttpStatus.BAD_REQUEST;
            message = "请求参数缺失: " + e.getMessage();
        } else if (e instanceof HttpRequestMethodNotSupportedException) {
            status = HttpStatus.METHOD_NOT_ALLOWED;
            message = "不支持的请求方法";
        } else if (e instanceof NoHandlerFoundException) {
            status = HttpStatus.NOT_FOUND;
            message = "接口不存在";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "内部错误！麻烦通过右上角的'反馈意见'功能反馈，谢谢！";
        }

        // JSON 默认 UTF-8（Spring Framework 6+ 使用 APPLICATION_JSON）
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 建议返回结构化错误信息
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("code", status.value());
        errorResponse.put("message", message);
        errorResponse.put("timestamp", new Date());

        return new ResponseEntity<>(errorResponse, headers, status);
    }
}
```

## 六、代码注释规范

### 6.1 类注释

```java
/*
 * Copyright (c) Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.coretool.ui.controller;

/**
 * 接口层描述
 *
 * @author 作者名
 * @since 2024-01-01
 */
public class ExampleController {
    // ...
}
```

### 6.2 方法注释

```java
/**
 * 方法功能描述
 *
 * @param param1 参数1说明
 * @param param2 参数2说明
 * @return 返回值说明
 */
public ResponseObject<Result> methodName(String param1, String param2) {
    // ...
}
```

### 6.3 字段注释

```java
/**
 * 是否成功
 */
private boolean success;

/**
 * 提示信息
 */
private String message;

/**
 * 具体业务数据
 */
private T data;
```

## 七、命名规范

### 7.1 包命名

- 全部小写
- 使用单词缩写或全拼
- 示例：`com.coretool.ui.controller`

### 7.2 类型命名（接口、类、枚举等）

- 使用**大驼峰**（PascalCase）
- Controller 类以 `Controller` 结尾
- Service 类以 `Service` 结尾
- Mapper 类以 `Mapper` 结尾
- DTO 类以 `Dto` 结尾
- VO 类以 `Request` 或 `Response` 结尾（或使用业务名称）
- **单元测试类**必须以 `Test` 结尾，例如：`ExampleServiceTest`、`ExampleControllerTest`

### 7.3 属性、局部变量、方法、参数命名

- 使用**小驼峰**（camelCase）
- 查询方法以 `query`、`get`、`find` 开头
- 创建方法以 `create`、`insert`、`add` 开头
- 更新方法以 `update`、`modify` 开头
- 删除方法以 `delete`、`remove` 开头

### 7.4 测试方法命名

- 默认仍推荐小驼峰；**允许**在测试方法名中使用下划线 `_` 提高可读性（如 `should_return_ok_when_input_valid`），与生产代码方法名区分策略由团队统一即可。

### 7.5 枚举值与静态常量

- 使用**全大写字母 + 下划线**分隔（`UPPER_SNAKE_CASE`）
- 示例：`MAX_RETRY`、`DEFAULT_PAGE_SIZE`、`STATUS_ACTIVE`

### 7.6 变量与布尔命名

- 局部变量、实例字段使用小驼峰
- 布尔类型建议以 `is`、`has`、`can` 等开头：`isValid`、`hasError`

### 7.7 条件分支：`else if` 须以 `else` 收尾

若存在 **`if` / `else if` 链**，链的**末尾必须有 `else`**，用于默认或穷尽分支，避免仅写到最后一个 `else if` 即结束。

```java
// 推荐：链末有 else
if (status == Status.PENDING) {
    // ...
} else if (status == Status.RUNNING) {
    // ...
} else {
    // 默认或其它状态
}

// 不推荐：else if 后直接结束而无 else（除非团队明确豁免的极简场景）
```

### 7.8 类型与强转

- **减少强制类型转换**（`(Type) obj`）；优先通过泛型、多态、`instanceof` 模式匹配、设计拆分等方法让类型在编译期明确。
- 若必须转换，应缩小作用域并配合 `instanceof` 或 API 层面的明确返回类型，避免深层嵌套强转。

### 7.9 异常类型命名

- 自定义**受检/非受检异常类**名称应以 **`Exception` 为后缀**（如 `BusinessException`、`OrderNotFoundException`）。
- 若使用 **`Error` 表示严重错误**（与 `Throwable` 体系一致），类名以 **`Error` 为后缀**（如 `ConfigError`）；**不要**使用无后缀的模糊命名（如 `BusinessFail`）。

## 八、常量定义规范

### 8.1 CommonConst 类

所有通用常量定义在 `CommonConst` 类中：

```java
public class CommonConst {
    // 成功/失败状态
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";

    // 字符串常量
    public static final String EMPTY_STRING = "";
    public static final String COMMA = ",";

    // 数字常量
    public static final int VALID = 0;
    public static final int INVALID = 1;

    // 时间格式
    public static final String TIME_DATE_LONG_FORMAT = "yyyy-MM-dd HH:mm:ss";
}
```

### 8.2 枚举类

枚举类放在 `constants.enums` 包下：

```java
package com.coretool.constants.enums;

public enum StatusEnum {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布");

    private final int code;
    private final String desc;

    StatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
```

## 九、Swagger 接口文档规范

### 9.1 类级别文档

```java
@RestController
@RequestMapping("/api/v1/examples")
@Tag(name = "ExampleController", description = "示例接口层")
public class ExampleController {
    // ...
}
```

### 9.2 方法级别文档

```java
@Operation(summary = "接口简要描述")
@GetMapping(produces = "application/json; charset=utf-8")
public ResponseObject<List<ExampleDto>> queryCollection(
    @RequestParam(value = "name", required = false) String name) {
    // ...
}
```

## 十、最佳实践

### 10.1 链式调用

```java
// 推荐：使用链式调用
return new ResponseObject<List<String>>().success(result);

// 不推荐：分步设置
ResponseObject<List<String>> response = new ResponseObject<>();
response.setSuccess(true);
response.setMessage(CommonConst.OK);
response.setData(result);
return response;
```

### 10.2 参数校验

```java
public ResponseObject<String> create(ExampleRequest request) {
    // 参数校验
    if (StringUtils.isEmpty(request.getName())) {
        return new ResponseObject<String>().failure("名称不能为空");
    }

    // 业务逻辑
    // ...
}
```

### 10.3 异常处理

```java
try {
    // 业务逻辑
    return new ResponseObject<String>().success("操作成功");
} catch (Exception e) {
    log.error("操作失败", e);
    return new ResponseObject<String>().failure("操作失败：" + e.getMessage());
}
```

### 10.4 日志记录

```java
// 记录关键操作日志
log.info("开始执行操作，参数：{}", request);

// 记录异常
log.error("操作失败，参数：{}", request, e);

// 记录警告
log.warn("数据为空，参数：{}", request);
```

## 十一、项目配置建议

### 11.1 pom.xml 基础配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.coretool</groupId>
    <artifactId>your-project-name</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>YourProjectName</name>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.1</version>
        <relativePath/>
    </parent>

    <properties>
        <!-- Java 21 配置 -->
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- OpenAPI / Swagger UI（Spring Boot 3，版本与 Boot 3.4 对齐，可随官方发布更新） -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.8.5</version>
        </dependency>

        <!-- MyBatis-Plus（Spring Boot 3） -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>3.5.9</version>
        </dependency>

        <!-- GaussDB 8.2（MySQL 兼容模式）：MySQL 8 驱动，版本以与数据库及 JDK 兼容为准 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Guava 工具包（显式版本，避免未受 Boot 管理时构建失败） -->
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>33.4.0-jre</version>
        </dependency>

        <!-- Apache Commons 工具包 -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>

        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-collections4</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Maven 编译插件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>

            <!-- Spring Boot Maven 插件 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 11.2 application.yml 配置

```yaml
# 服务端口
server:
  port: 8080

# GaussDB 8.2（MySQL 兼容模式）数据源；URL、账号密码由部署环境提供
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://<host>:<port>/<database>?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: <username>
    password: <password>

# Swagger 配置
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

# 日志配置
logging:
  level:
    com.coretool: INFO
```

## 总结

本规范定义了后端项目的统一开发标准，包括：

### 技术栈规范
- **Java 版本**：Java 21（LTS），使用 Lambda、Stream、`java.time`、Optional，以及按需使用的 `var`、`record`、文本块、模式匹配等
- **Spring Boot 版本**：3.4.x（Jakarta EE 命名空间、Spring Framework 6）
- **架构范式**：**DDD（领域驱动设计）**，领域层采用**充血模型**
- **数据库**：**GaussDB 8.2**（**MySQL 8.0 兼容语法**；JDBC：`mysql-connector-j`，连接参数以部署环境为准）
- **核心依赖**：Lombok、SpringDoc OpenAPI、**MyBatis-Plus**、**GaussDB 8.2（mysql-connector-j）**、Guava 等

### 开发规范
1. **接口返回对象**：使用 `ResponseObject<T>` 统一返回格式
2. **Controller 层**：标准注解、参数处理、依赖注入规范；**URL 须符合 RESTful**（名词、复数资源、HTTP 方法语义化，见 2.3～2.4 节）
3. **领域与应用层**：**DDD 领域驱动设计** + **充血模型**；`domain` 承载实体/值对象/聚合行为与仓储接口，`application/service` 薄编排、事务边界与基础设施协作（见「DDD 领域驱动设计与充血模型」）
4. **Service 层（Application）**：用例编排、异常处理、日志记录规范；复杂规则委托领域对象，避免在应用服务中堆砌贫血 `setXxx` 逻辑
5. **VO 对象**：使用 Lombok 简化代码（**VO/DTO 可与领域对象分离**，领域模型不随 UI 泄漏）
6. **异常处理**：全局异常处理器统一处理；自定义异常类名以 `Exception` / `Error` 为后缀
7. **代码注释**：规范的类、方法、字段注释
8. **命名规范**：包、类、枚举（大驼峰）、测试类 `Test` 后缀、成员与方法（小驼峰）、测试方法可含 `_`、枚举值与静态常量（全大写下划线）
9. **常量定义**：CommonConst 和枚举类使用规范
10. **Swagger 文档**：接口文档注解规范
11. **最佳实践**：链式调用、参数校验、异常处理、日志记录
12. **Java 21 特性**：合理使用 Lambda、Stream、Optional、日期时间 API及现代语法（控制复杂度与函数长度）
13. **规模与复杂度**：新增函数圈复杂度 ≤20、单函数 ≤50 行、单文件 ≤2000 行
14. **分支与类型**：`else if` 链须以 `else` 结尾；减少强转，类型尽量在编译期明确
15. **数据库与持久化**：**GaussDB 8.2** + **MyBatis-Plus**，MySQL 8.0 兼容语法；分页 `LIMIT offset, size`；禁止复杂原生 SQL及存储过程、触发器、外键等（见「数据库规范（华为高斯 GaussDB 8.2）」）

### Java 21 使用要点
- ✅ 使用 Lambda、Stream、`java.time`、`Optional`
- ✅ Spring Boot 3 使用 `jakarta.*`（如 `jakarta.annotation.Resource`、`jakarta.servlet.*`）
- ✅ 按需使用 `var`、`record`、文本块、`Stream.toList()`、模式匹配等，以可读性为先
- ✅ 自定义异常类名以 `Exception` 或 `Error` 结尾
- ❌ 避免不必要的强制类型转换与无后缀的异常类名

遵循本规范可以确保代码风格统一、接口格式一致、易于维护和扩展，并在 Java 21 + Spring Boot 3 技术栈与 **DDD 充血模型** 下保持可演进性。
