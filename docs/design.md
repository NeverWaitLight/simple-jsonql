# 系统设计文档 - Engine

- [ ] sql cache

## 设计理念与核心概念

Engine 旨在提供一种统一且灵活的方式，通过特定格式的 JSON 查询语言来操作关系型数据库。系统核心理念是**将 CRUD 操作抽象为声明式的 JSON 结构**，然后将这些结构转换为 SQL 语句执行，从而实现数据操作的标准化和简化。

### 核心设计目标

- **统一接口**：通过标准化的 JSON 结构，为各种客户端应用提供一致的数据访问接口
- **类型安全**：通过明确定义的 JSON 结构和字段验证，减少运行时错误
- **操作抽象**：将通用的 CRUD 操作抽象为声明式结构，隐藏 SQL 复杂性
- **可扩展性**：基于抽象基类和接口，便于扩展新的操作类型和查询能力

### 关键抽象概念

- **Statement**：所有操作的抽象基础，包含操作类型（QUERY, CREATE, UPDATE, DELETE）
- **Engine**：负责将 Statement 转换为 SQL 并执行，根据不同操作类型有专门的实现
- **Clause**：SQL 子句的抽象，如 WHERE、JOIN、ORDER BY 等
- **Parser**：负责将 JSON 字符串解析为 Statement 对象的组件

## 系统架构

```mermaid
flowchart LR
    A[客户端] -->|HTTP/JSON| B[Controller层]
    B --> C[Service层]
    C --> D[Engine层]
    D -->|JDBC| E[数据库]
    D --> F[Parser层]
    D --> G[Metadata层]
    F --> H[Statement Model]
    G --> H

    subgraph 应用程序
    B
    C
    D
    F
    G
    H
    end
```

**架构说明:**

- **客户端**: 通过 HTTP 发送结构化的 JsonQL 请求。
- **Controller 层**: 接收 HTTP 请求，使用 DTO 封装请求和响应数据，调用 Service 层处理业务逻辑。
- **Service 层**: 实现业务逻辑，协调 JsonQLEngine 进行实际的数据库操作。
- **Engine 层**: 系统的核心，负责解析和执行 JsonQL：
  - 基于**策略模式**设计，通过 StatementEngine 接口提供统一 API
  - 各种操作类型有专门的 Engine 实现，实现代码复用的同时支持特定逻辑
  - 使用专门的 SqlParser 处理 SQL 转换
  - 使用**策略模式**处理不同类型的 SQL 子句（WHERE, JOIN 等）
- **Parser 层**: 负责 JSON 到 Statement 对象的转换，利用类型系统确保结构正确。
- **Statement Model**: 定义 JsonQL 的语法结构和组件，是系统的"语言规范"。
- **Metadata 层**: 管理数据实体的元数据，支持实体与表的映射关系。
- **数据库**: 持久化存储数据的关系型数据库（当前配置为 MySQL）。

## CRUD 抽象与实现

JsonQL 系统通过抽象基类和专门的实现，实现了 CRUD 操作的共性复用和特性支持：

### 统一抽象层

```mermaid
classDiagram
    class JsonQLStatement {
        <<abstract>>
        +String appId
        +String formId
        +String entityId
        +StatementType statement
    }

    class StatementEngine {
        <<abstract>>
        #MetadataSources metadataSources
        +Object execute(Connection, JsonQLStatement)
    }

    class SqlParser {
        <<interface>>
        +PreparedSql parseStmt2Sql(JsonQLStatement)
    }

    JsonQLStatement <|-- QueryStatement
    JsonQLStatement <|-- CreateStatement
    JsonQLStatement <|-- UpdateStatement
    JsonQLStatement <|-- DeleteStatement

    StatementEngine <|-- QueryEngine
    StatementEngine <|-- CreateEngine
    StatementEngine <|-- UpdateEngine
    StatementEngine <|-- DeleteEngine

    SqlParser <|-- QuerySqlParser
    SqlParser <|-- CreateSqlParser
    SqlParser <|-- UpdateSqlParser
    SqlParser <|-- DeleteSqlParser

    QueryEngine --> QuerySqlParser : 使用
    CreateEngine --> CreateSqlParser : 使用
    UpdateEngine --> UpdateSqlParser : 使用
    DeleteEngine --> DeleteSqlParser : 使用

    QueryEngine --> QueryStatement : 处理
    CreateEngine --> CreateStatement : 处理
    UpdateEngine --> UpdateStatement : 处理
    DeleteEngine --> DeleteStatement : 处理
```

- **JsonQLStatement**：所有声明式操作的基类，包含通用属性
- **StatementEngine**：所有执行器的基类，定义了执行流程
- **SqlParser**：SQL 转换接口，负责将 Statement 转换为 PreparedSql

### CRUD 特性实现

各类操作在共享抽象的同时，也有其特定的功能和结构：

- **查询操作 (Query)**

  - 支持条件过滤、排序、分页
  - 支持字段选择和关联查询
  - 特有组件：Filter, Sort, Page

- **创建操作 (Create)**

  - 支持多字段同时创建
  - 支持默认值和系统字段（创建时间等）
  - 特有组件：Field 集合

- **更新操作 (Update)**

  - 支持条件更新和主键更新
  - 支持部分字段更新
  - 特有组件：主键 ID 和 Field 集合

- **删除操作 (Delete)**
  - 支持条件删除和主键删除
  - 支持批量删除和软删除
  - 特有组件：ID 集合和 Filter

### SQL 解析与生成

每种操作类型都有特定的 SQL 解析器：

- **基础解析器**：

  - `SqlParser`：通用解析接口
  - `PreparedSql`：封装 SQL 语句和参数

- **子句解析器**：

  - `ClauseSqlParser`：子句处理接口
  - `WhereClauseSqlParser`：条件子句解析
  - `JoinClauseSqlParser`：连接子句解析
  - `OrderByClauseSqlParser`：排序子句解析
  - `LimitClauseSqlParser`：分页子句解析

- **语句解析器**：
  - `QuerySqlParser`：查询语句解析
  - `CreateSqlParser`：创建语句解析
  - `UpdateSqlParser`：更新语句解析
  - `DeleteSqlParser`：删除语句解析

## 模块详细说明

### `engine` - 执行引擎核心

- **`JsonQLEngine`**: 核心协调器，负责:

  - 管理各类 StatementEngine 实例
  - 协调执行流程
  - 处理连接和事务

- **`StatementEngine` (抽象基类)**:

  - 定义执行方法
  - 封装通用执行逻辑

- **CRUD 执行引擎**:

  - `QueryEngine`: 实现 SELECT 语句执行和结果集处理
  - `CreateEngine`: 实现 INSERT 语句执行和新增 ID 返回
  - `UpdateEngine`: 实现 UPDATE 语句执行和影响行计数
  - `DeleteEngine`: 实现 DELETE 语句执行和影响行计数

- **SQL 解析组件**:

  - `SqlParser`: SQL 解析接口
  - `PreparedSql`: 封装 SQL 和参数的数据容器，支持类型安全的参数传递
  - `WhereClauseSqlParser`: 处理 WHERE 条件转换
  - `JoinClauseSqlParser`: 处理 JOIN 转换
  - `OrderByClauseSqlParser`: 处理排序转换
  - `LimitClauseSqlParser`: 处理分页限制

### `statement` - 声明式结构定义

- **`StatementParser`**:

  - 使用 Jackson 进行 JSON 解析
  - 负责类型验证和对象构建
  - 处理不同操作类型分发

- **CRUD 声明类**:

  - `QueryStatement`: 定义查询结构（条件、排序、分页）
  - `CreateStatement`: 定义创建结构（字段集合）
  - `UpdateStatement`: 定义更新结构（ID 和字段集合）
  - `DeleteStatement`: 定义删除结构（ID 集合或条件）

```mermaid
graph TD
    A[MetadataSources] --> |buildMetadata| B[Metadata]
    A --> |使用| C[EntityMetadataBuilder]
    C --> |bindEntity| D[PersistentClass]
    B --> |contains| D
    G[MetadataException] --> |被各类使用| A
    G --> |被各类使用| B
    G --> |被各类使用| C
```

## CRUD 操作流程

### 统一执行流程

无论何种操作类型，都遵循以下基本流程：

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant JsonQLEngine
    participant XxxEngine
    participant SqlParser
    participant Database

    Client->>Controller: 发送JsonQL请求
    Controller->>Service: 调用服务方法
    Service->>JsonQLEngine: execute(jsonQuery)
    JsonQLEngine->>StatementParser: parse2Stmt(jsonQuery)
    StatementParser-->>JsonQLEngine: XxxStatement
    JsonQLEngine->>XxxEngine: execute(conn, statement)
    XxxEngine->>SqlParser: parseStmt2Sql(statement)
    SqlParser-->>XxxEngine: PreparedSql
    XxxEngine->>Database: 执行SQL
    Database-->>XxxEngine: 返回结果
    XxxEngine-->>JsonQLEngine: 处理结果
    JsonQLEngine-->>Service: 返回结果
    Service-->>Controller: 返回结果
    Controller-->>Client: 返回HTTP响应
```

### 各操作特有处理

各类操作在遵循统一流程的基础上，有其特定的处理逻辑：

- **查询 (Query)**:
  - 将 Filter 条件转换为 WHERE 子句
  - 将 Sort 转换为 ORDER BY 子句
  - 将 Page 转换为 LIMIT/OFFSET 子句
  - 处理 ResultSet 映射为 List<Map>
  - 支持关联字段处理（处理格式: relation_field）
- **创建 (Create)**:
  - 将 Field 列表转换为 INSERT 的列和值
  - 处理自增 ID 返回
  - 构建完整的返回对象
- **更新 (Update)**:
  - 将 Field 列表转换为 SET 子句
  - 处理 WHERE 条件（ID 或自定义条件）
  - 返回更新后的数据或影响行数
- **删除 (Delete)**:
  - 处理 WHERE 条件（ID 列表或自定义条件）
  - 返回影响行数

## 未来优化与发展方向

- **复杂查询增强**:
  - 支持更复杂的 JOIN 操作和子查询
  - 增加聚合函数支持
  - 支持复杂的分组操作
- **SQL 优化**: 通过动态分析和优化器提升性能。
- **批量操作**: 优化批量创建、更新和删除的性能。
- **事务支持**: 增强事务管理功能，支持多操作事务。
- **ORM 增强**: 完善对象-关系映射，支持实体类型自动映射。
- **安全增强**:
  - 字段级别权限控制
  - 数据行级过滤
  - 更严格的输入验证
- **元数据驱动**: 从元数据自动生成操作约束和验证规则。
- **全面测试**: 添加单元测试和集成测试，确保各模块功能正确和系统整体稳定性。
