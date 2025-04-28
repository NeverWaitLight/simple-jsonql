# 创建

```http
POST /api/v1/data/create

Authorization: Bearer {token}
Content-Type: application/json

{
  "appId": "123456",
  "formId": "89757",
  "entityId": "147258",
  "fields": [
    {"field": "name", "value": "tom"},
    {
      "field": "blogs",
      "values": [
        {
          "appId": "123456",
          "formId": "89758",
          "entityId": "147259",
          "fields": [ {"field": "title", "value": "太阳照常升起"} ]
        },
        {
          "appId": "123456",
          "formId": "89758",
          "entityId": "147259",
          "fields": [ {"field": "title", "value": "活着"} ]
        }
      ]
    }
  ]
}
```

## 请求参数说明

| 参数名        | 类型   | 必填 | 描述                         |
| ------------- | ------ | ---- | ---------------------------- |
| appId         | String | 是   | 应用ID                       |
| formId        | String | 是   | 表单ID                       |
| entityId      | String | 是   | 实体ID                       |
| fields        | Array  | 是   | 字段数组                     |
| fields.field  | String | 是   | 字段名称                     |
| fields.value  | Any    | 否   | 字段值，单值类型字段使用     |
| fields.values | Array  | 否   | 字段值数组，多值类型字段使用 |

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 52

{
  "code": 200,
  "message": "OK",
  "data": {
    "id": "1",
    "name": "tom",
    "blogs": [
      {
        "id": "321",
        "title": "太阳照常升起",
        "createdBy": "admin",
        "createdAt": "2025-01-01 12:30:11",
        "updatedBy": "admin",
        "updatedAt": "2025-01-01 12:30:11"
      },
      {
        "id": "322",
        "title": "活着",
        "createdBy": "admin",
        "createdAt": "2025-01-01 12:30:11",
        "updatedBy": "admin",
        "updatedAt": "2025-01-01 12:30:11"
      }
    ],
    "createdBy": "admin",
    "createdAt": "2025-01-01 12:30:11",
    "updatedBy": "admin",
    "updatedAt": "2025-01-01 12:30:11"
  }
}
```

## 响应参数说明

| 参数名                 | 类型   | 描述                |
| ---------------------- | ------ | ------------------- |
| code                   | Number | 状态码，200表示成功 |
| message                | String | 状态描述            |
| data                   | Object | 返回的数据对象      |
| data.id                | String | 记录ID              |
| data.name              | String | 记录名称            |
| data.blogs             | Array  | 博客记录数组        |
| data.blogs[].id        | String | 博客记录ID          |
| data.blogs[].title     | String | 博客标题            |
| data.blogs[].createdBy | String | 创建者              |
| data.blogs[].createdAt | String | 创建时间            |
| data.blogs[].updatedBy | String | 更新者              |
| data.blogs[].updatedAt | String | 更新时间            |
| data.createdBy         | String | 主记录创建者        |
| data.createdAt         | String | 主记录创建时间      |
| data.updatedBy         | String | 主记录更新者        |
| data.updatedAt         | String | 主记录更新时间      |

# 删除

```http
POST /api/v1/data/remove

Authorization: Bearer {token}
Content-Type: application/json

{
  "appId": "123456",
  "formId": "89757",
  "entityId": "89757",
  "ids": [
    "1"
  ]
}
```

## 请求参数说明

| 参数名   | 类型   | 必填 | 描述               |
| -------- | ------ | ---- | ------------------ |
| appId    | String | 是   | 应用ID             |
| formId   | String | 是   | 表单ID             |
| entityId | String | 是   | 实体ID             |
| ids      | Array  | 是   | 要删除的数据ID数组 |

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 52

{
    "count": 1
}
```

## 响应参数说明

| 参数名 | 类型   | 描述           |
| ------ | ------ | -------------- |
| count  | Number | 删除的记录数量 |

# 修改

```http
POST /api/v1/data/update

Authorization: Bearer {token}
Content-Type: application/json

{
  "appId": "123456",
  "formId": "89757",
  "entityId": "147258",
  "dataId": "1",
  "fields": [
    {"field": "name", "value": "高桥凉介"},
    {
      "field": "blogs",
      "values": [
        {
          "appId": "123456",
          "formId": "89758",
          "entityId": "147259",
          "dataId": "321",
          "fields": [ {"field": "title", "value": "生死疲劳"} ]
        }
      ]
    }
  ]
}
```

## 请求参数说明

| 参数名        | 类型   | 必填 | 描述                         |
| ------------- | ------ | ---- | ---------------------------- |
| appId         | String | 是   | 应用ID                       |
| formId        | String | 是   | 表单ID                       |
| entityId      | String | 是   | 实体ID                       |
| dataId        | String | 是   | 要修改的数据ID               |
| fields        | Array  | 是   | 字段数组                     |
| fields.field  | String | 是   | 字段名称                     |
| fields.value  | Any    | 否   | 字段值，单值类型字段使用     |
| fields.values | Array  | 否   | 字段值数组，多值类型字段使用 |

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 52

{
  "code": 200,
  "message": "OK",
  "data": {
    "id": "1",
    "name": "高桥凉介",
    "blogs": [
      {
        "id": "321",
        "title": "生死疲劳",
        "createdBy": "admin",
        "createdAt": "2025-01-01 12:30:11",
        "updatedBy": "admin",
        "updatedAt": "2025-01-01 12:30:11"
      }
    ],
    "createdBy": "admin",
    "createdAt": "2025-01-01 12:30:11",
    "updatedBy": "admin",
    "updatedAt": "2025-01-01 12:30:11"
  }
}
```

## 响应参数说明

| 参数名                 | 类型   | 描述                |
| ---------------------- | ------ | ------------------- |
| code                   | Number | 状态码，200表示成功 |
| message                | String | 状态描述            |
| data                   | Object | 返回的数据对象      |
| data.id                | String | 记录ID              |
| data.name              | String | 记录名称            |
| data.blogs             | Array  | 博客记录数组        |
| data.blogs[].id        | String | 博客记录ID          |
| data.blogs[].title     | String | 博客标题            |
| data.blogs[].createdBy | String | 创建者              |
| data.blogs[].createdAt | String | 创建时间            |
| data.blogs[].updatedBy | String | 更新者              |
| data.blogs[].updatedAt | String | 更新时间            |
| data.createdBy         | String | 主记录创建者        |
| data.createdAt         | String | 主记录创建时间      |
| data.updatedBy         | String | 主记录更新者        |
| data.updatedAt         | String | 主记录更新时间      |

# 查询

```http
POST /api/v1/data/page
Authorization: Bearer {token}
Content-Type: application/json

{
  "appId": "123456",
  "formId": "89757",
  "entityId": "89757",
  "filters": {
    "rel": "or",
    "conditions": [
      {"field": "status", "method": "eq", "value": "active"}   ,
      { "field": "name", "method": "in", "values": ["A", "B"] }
    ]
  },
  "sort": [
    {"field": "name"      , "direction": "DESC"},
    {"field": "createTime", "direction": "ASC" }
  ],
  "page": {"size": 20, "number": 1}
}
```

## 请求参数说明

| 参数名                      | 类型   | 必填 | 描述                             |
| --------------------------- | ------ | ---- | -------------------------------- |
| appId                       | String | 是   | 应用ID                           |
| formId                      | String | 是   | 表单ID                           |
| entityId                    | String | 是   | 实体ID                           |
| filters                     | Object | 否   | 过滤条件                         |
| filters.rel                 | String | 是   | 条件关系，"and"或"or"            |
| filters.conditions          | Array  | 是   | 条件数组                         |
| filters.conditions[].field  | String | 是   | 字段名称                         |
| filters.conditions[].method | String | 是   | 匹配方法，如"eq"、"in"、"like"等 |
| filters.conditions[].value  | Any    | 否   | 匹配值，单值匹配时使用           |
| filters.conditions[].values | Array  | 否   | 匹配值数组，多值匹配时使用       |
| sort                        | Array  | 否   | 排序条件                         |
| sort[].field                | String | 是   | 排序字段                         |
| sort[].direction            | String | 是   | 排序方向，"ASC"或"DESC"          |
| page                        | Object | 否   | 分页信息                         |
| page.size                   | Number | 是   | 每页记录数                       |
| page.number                 | Number | 是   | 页码，从1开始                    |

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 52

{
  "code": 200,
  "message": "OK",
  "data": [
    {
      "id": "1",
      "name": "tom",
      "blogs": [ {"id": "321", "title": "活着"} ],
      "createdBy": "admin",
      "createdAt": "2025-01-01 12:30:11",
      "updatedBy": "admin",
      "updatedAt": "2025-01-01 12:30:11"
    }
  ],
  "total": 1,
  "page": 1,
  "size": 10,
  "totalPages": 1
}
```

## 子表查询

```http
POST /api/v1/data/page
Authorization: Bearer {token}
Content-Type: application/json

{
  "appId": "123456",
  "formId": "89757",
  "entityId": "89757",
  "filters": {
    "rel": "and",
    "conditions": [
      {"field": "name"       , "method": "eq"  , "value": "tom"},
      {"field": "blogs.title", "method": "like", "value": "活"  }
    ]
  },
  "sort": [
    {"field": "name"     , "direction": "ASC" },
    {"field": "createdAt", "direction": "DESC"}
  ],
  "page": {"size": 10, "number": 1}
}
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "code": 200,
  "message": "OK",
  "data": [
    {
      "id": "1",
      "name": "tom",
      "blogs": [ {"id": "321", "title": "活着"} ],
      "createdBy": "admin",
      "createdAt": "2025-01-01 12:30:11",
      "updatedBy": "admin",
      "updatedAt": "2025-01-01 12:30:11"
    }
  ],
  "total": 1,
  "page": 1,
  "size": 10,
  "totalPages": 1
}
```

