# JSONQL 的 CRUD 核心语法

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
    "age": {
      "gt": 18
    },
    "status": "active"
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
    "id": 123
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
    "status": "inactive",
    "created_at": {
      "lt": "2025-01-01"
    }
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
    "user.age": {
      "gt": 18
    }
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
      "field": "COUNT(*)",
      "alias": "人数"
    }
  ],
  "from": "employee",
  "groupBy": [
    "department"
  ],
  "having": {
    "COUNT(*)": {
      "gt": 5
    }
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
    "name": "$param.name"
  }
}
```

执行流程：

- 前端传入 `{ "param": { "name": "李四" } }`；
- 生成 `SELECT * FROM user WHERE name = ?`，参数绑定为 `李四`。

## 数据校验规则

在 JSONQL 中嵌入校验逻辑，确保数据合法性：

```json
{
  "statement": "insert",
  "into": "product",
  "values": {
    "price": 150,
    "validations": {
      "minimum": 100,
      "maximum": 1000
    }
  }
}
```

校验结果：

- 若 `price` 小于 100 或大于 1000，插入操作自动终止并返回错误。


