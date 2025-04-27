# JSONQL Protocol Specification

JSONQL 通过 JSON 格式封装 SQL 语义，支持标准增删改查操作。以下为不同操作的语法示例：

## 查询数据（SELECT）

```json
{
  "statement": "select",
  "select": [
    "id",
    "name",
    "age"
  ],
  "from": "user",
  "where": {
    "type": "logical",
    "operator": "and",
    "conditions": [
      {
        "type": "comparison",
        "field": "age",
        "operator": "gt",
        "value": 18
      },
      {
        "type": "comparison",
        "field": "status",
        "operator": "eq",
        "value": "active"
      }
    ]
  },
  "orderBy": {
    "field": "age",
    "direction": "desc"
  },
  "limit": 10
}
```

等效 SQL：

```sql
SELECT id, name, age FROM user 
WHERE age > 18 AND status = 'active' 
ORDER BY age DESC LIMIT 10;
```

应用场景：

- 获取用户列表，仅返回必要字段，避免全字段查询导致的性能问题；
- 支持复杂条件嵌套（如 `AND`/`OR` 组合）和跨表关联查询。

## 插入数据（INSERT）

```json
{
  "statement": "insert",
  "into": "user",
  "values": {
    "name": "张三",
    "age": 25,
    "email": "zhangsan@example.com"
  }
}
```

等效 SQL：

```sql
INSERT INTO user (name, age, email) 
VALUES ('张三', 25, 'zhangsan@example.com');
```

技术特性：

- 自动绑定参数化变量，防止 SQL 注入攻击；
- 支持批量插入（通过数组形式的 `values` 字段）。

## 更新数据（UPDATE）

```json
{
  "statement": "update",
  "update": "user",
  "set": {
    "age": 26,
    "status": "inactive"
  },
  "where": {
    "type": "comparison",
    "field": "id",
    "operator": "eq",
    "value": 123
  }
}
```

等效 SQL：

```sql
UPDATE user 
SET age = 26, status = 'inactive' 
WHERE id = 123;
```

安全机制：

- 更新操作默认开启事务，确保原子性；
- 支持条件校验（如 `status` 变更需满足前置状态）。

## 删除数据（DELETE）

```json
{
  "statement": "delete",
  "from": "user",
  "where": {
    "type": "logical",
    "operator": "and",
    "conditions": [
      {
        "type": "comparison",
        "field": "status",
        "operator": "eq",
        "value": "inactive"
      },
      {
        "type": "comparison",
        "field": "created_at",
        "operator": "lt",
        "value": "2025-01-01"
      }
    ]
  }
}
```

等效 SQL：

```sql
DELETE FROM user 
WHERE status = 'inactive' AND created_at < '2025-01-01';
```

优化策略：

- 对批量删除操作限制 `LIMIT` 数量，避免误删全表；
- 记录操作日志，支持回滚和数据追溯。

# WHERE 条件说明

## 条件类型

所有条件都遵循以下基本格式：
```json
{
  "type": "条件类型",
  "field": "字段名",
  "operator": "操作符",
  "value": "值",
  "conditions": [] // 用于逻辑操作
}
```

### 类型（type）取值范围

1. `comparison`: 比较操作，用于字段与值的比较
2. `logical`: 逻辑操作，用于组合多个条件
3. `subquery`: 子查询，用于嵌套查询条件

### 操作符（operator）取值范围

1. 比较操作符（type: comparison）：
   - `eq`: 等于 (Equal)
   - `ne`: 不等于 (Not Equal)
   - `gt`: 大于 (Greater Than)
   - `ge`: 大于等于 (Greater than or Equal)
   - `lt`: 小于 (Less Than)
   - `le`: 小于等于 (Less than or Equal)
   - `like`: 模糊匹配
   - `in`: 包含于列表
   - `between`: 范围查询
   - `is`: 空值判断
   - `exists`: 存在性判断

2. 逻辑操作符（type: logical）：
   - `and`: 与
   - `or`: 或
   - `not`: 非

### 1. 基本比较操作
```json
{
  "type": "comparison",
  "field": "age",
  "operator": "gt",
  "value": 18
}
```
等效 SQL：`WHERE age > 18`

### 2. 逻辑操作
```json
{
  "type": "logical",
  "operator": "and",
  "conditions": [
    {
      "type": "comparison",
      "field": "age",
      "operator": "gt",
      "value": 18
    },
    {
      "type": "comparison",
      "field": "status",
      "operator": "eq",
      "value": "active"
    }
  ]
}
```
等效 SQL：`WHERE (age > 18 AND status = 'active')`

### 3. 范围查询
```json
{
  "type": "comparison",
  "field": "age",
  "operator": "between",
  "value": [18, 60]
}
```
等效 SQL：`WHERE age BETWEEN 18 AND 60`

### 4. 空值判断
```json
{
  "type": "comparison",
  "field": "email",
  "operator": "is",
  "value": null,
  "not": true
}
```
等效 SQL：`WHERE email IS NOT NULL`

### 5. 模糊匹配
```json
{
  "type": "comparison",
  "field": "name",
  "operator": "like",
  "value": "张%",
  "not": false
}
```
等效 SQL：`WHERE name LIKE '张%'`

### 6. IN 查询
```json
{
  "type": "comparison",
  "field": "status",
  "operator": "in",
  "value": ["active", "pending"],
  "not": false
}
```
等效 SQL：`WHERE status IN ('active', 'pending')`

### 7. 子查询
```json
{
  "type": "comparison",
  "field": "user_id",
  "operator": "exists",
  "value": {
    "type": "subquery",
    "select": ["1"],
    "from": "order",
    "where": {
      "type": "comparison",
      "field": "user_id",
      "operator": "eq",
      "value": "$user.id"
    }
  },
  "not": false
}
```
等效 SQL：`WHERE EXISTS (SELECT 1 FROM order WHERE user_id = user.id)`

# 高级功能与性能优化

## 跨表关联查询

通过 `join` 字段实现多表联合查询：

```json
{
  "statement": "select",
  "select": [
    "user.name",
    "order.product"
  ],
  "from": "user",
  "join": {
    "order": {
      "on": "user.id = order.user_id",
      "type": "inner"
    }
  },
  "where": {
    "type": "comparison",
    "field": "user.age",
    "operator": "gt",
    "value": 18
  }
}
```

等效 SQL：

```sql
SELECT user.name, order.product 
FROM user 
INNER JOIN order ON user.id = order.user_id 
WHERE user.age > 18;
```

## 聚合与分组统计

支持 `GROUP BY` 和聚合函数：

```json
{
  "statement": "select",
  "select": [
    {
      "field": "department",
      "alias": "部门"
    },
    {
      "type": "function",
      "name": "COUNT",
      "args": ["*"],
      "alias": "人数"
    }
  ],
  "from": "employee",
  "groupBy": [
    "department"
  ],
  "having": {
    "type": "comparison",
    "field": "COUNT(*)",
    "operator": "gt",
    "value": 5
  }
}
```

等效 SQL：

```sql
SELECT department AS 部门, COUNT(*) AS 人数 
FROM employee 
GROUP BY department 
HAVING COUNT(*) > 5;
```

## 分页性能优化

对大偏移量（`OFFSET`）查询进行 SQL 改写，提升性能：

```json
{
  "statement": "select",
  "select": [
    "film_id",
    "description"
  ],
  "from": "film",
  "orderBy": {
    "field": "title"
  },
  "limit": 10,
  "offset": 10000
}
```

优化后 SQL：

```sql
SELECT film_id, description 
FROM film 
INNER JOIN (
  SELECT film_id FROM film 
  ORDER BY title LIMIT 10 OFFSET 10000
) AS OFFSETOPT USING(film_id);
```

原理：

- 子查询仅获取主键（`film_id`），减少 IO 开销；
- 主查询通过 `INNER JOIN` 快速定位目标数据。

# 安全与验证机制

## 参数化查询

所有输入值自动转为预编译参数，避免 SQL 注入：

```json
{
  "statement": "select",
  "from": "user",
  "where": {
    "type": "comparison",
    "field": "name",
    "operator": "eq",
    "value": "$param.name"
  }
}
```

执行流程：

- 前端传入 `{ "param": { "name": "李四" } }`；
- 生成 `SELECT * FROM user WHERE name = ?`，参数绑定为 `李四`。