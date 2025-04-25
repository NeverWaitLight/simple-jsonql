# JSONQL WHERE 条件说明

本文档详细说明了 JSONQL 中 WHERE 条件的各种用法和实现方式。

## 条件类型

### 1. 基本比较操作
```json
{
  "type": "comparison",
  "field": "age",
  "operator": ">",
  "value": 18
}
```
等效 SQL：`WHERE age > 18`

### 2. 逻辑操作
```json
{
  "type": "logical",
  "operator": "AND",
  "conditions": [
    {
      "type": "comparison",
      "field": "age",
      "operator": ">",
      "value": 18
    },
    {
      "type": "comparison",
      "field": "status",
      "operator": "=",
      "value": "active"
    }
  ]
}
```
等效 SQL：`WHERE (age > 18 AND status = 'active')`

### 3. 范围查询
```json
{
  "type": "between",
  "field": "age",
  "values": [18, 60]
}
```
等效 SQL：`WHERE age BETWEEN 18 AND 60`

### 4. 空值判断
```json
{
  "type": "null",
  "field": "email",
  "not": true
}
```
等效 SQL：`WHERE email IS NOT NULL`

### 5. 模糊匹配
```json
{
  "type": "like",
  "field": "name",
  "pattern": "张%",
  "not": false
}
```
等效 SQL：`WHERE name LIKE '张%'`

### 6. IN 查询
```json
{
  "type": "in",
  "field": "status",
  "values": ["active", "pending"],
  "not": false
}
```
等效 SQL：`WHERE status IN ('active', 'pending')`

### 7. 子查询
```json
{
  "type": "exists",
  "subquery": {
    "statement": "select",
    "select": ["1"],
    "from": "order",
    "where": {
      "type": "comparison",
      "field": "user_id",
      "operator": "=",
      "value": "$user.id"
    }
  },
  "not": false
}
```
等效 SQL：`WHERE EXISTS (SELECT 1 FROM order WHERE user_id = user.id)`

## 复杂条件组合示例

### 示例 1：多条件组合
```json
{
  "type": "logical",
  "operator": "AND",
  "conditions": [
    {
      "type": "comparison",
      "field": "age",
      "operator": ">",
      "value": 18
    },
    {
      "type": "in",
      "field": "status",
      "values": ["active", "pending"]
    },
    {
      "type": "null",
      "field": "email",
      "not": true
    }
  ]
}
```
等效 SQL：`WHERE (age > 18 AND status IN ('active', 'pending') AND email IS NOT NULL)`

### 示例 2：嵌套逻辑
```json
{
  "type": "logical",
  "operator": "OR",
  "conditions": [
    {
      "type": "logical",
      "operator": "AND",
      "conditions": [
        {
          "type": "comparison",
          "field": "age",
          "operator": ">",
          "value": 18
        },
        {
          "type": "comparison",
          "field": "status",
          "operator": "=",
          "value": "active"
        }
      ]
    },
    {
      "type": "logical",
      "operator": "AND",
      "conditions": [
        {
          "type": "comparison",
          "field": "age",
          "operator": "<",
          "value": 60
        },
        {
          "type": "comparison",
          "field": "status",
          "operator": "=",
          "value": "pending"
        }
      ]
    }
  ]
}
```
等效 SQL：`WHERE ((age > 18 AND status = 'active') OR (age < 60 AND status = 'pending'))`

## 实现说明

所有条件类型都继承自 `Condition` 抽象类，并实现了 `toSql()` 方法来生成对应的 SQL 片段。主要特点：

1. 使用 Jackson 注解支持 JSON 序列化和反序列化
2. 每种条件类型都有明确的类型标识
3. 支持条件的嵌套和组合
4. 自动处理字符串值的引号
5. 支持参数化查询

## 使用建议

1. 优先使用基本比较操作，避免过度复杂的条件组合
2. 合理使用括号来明确条件的优先级
3. 注意字符串值的引号处理
4. 使用参数化查询来防止 SQL 注入
5. 对于复杂的条件组合，建议使用逻辑条件来组织 