# Simple JSONQL

一个基于 Spring Boot 的 JSON 格式 SQL 查询引擎，支持通过 JSON 格式构建 SQL 语句。

## 技术栈

- JDK 版本：21
- Spring Boot 版本：3.4.4
- 主要依赖：
  - Spring Boot Starter Data JDBC
  - Spring Boot Starter Data JPA
  - Spring Boot Starter Web
  - Lombok
  - MySQL Connector/J
  - Jackson Databind
  - JUnit 5 (测试框架)

## 项目说明

本项目提供了一个简单而强大的 JSON 格式 SQL 查询引擎，允许开发者通过 JSON 格式来构建和执行 SQL 语句。主要特性包括：

- 支持标准的 CRUD 操作
- 参数化查询，防止 SQL 注入
- 支持复杂的条件查询和关联查询
- 支持分页和排序
- 支持事务管理
- 完整的日志记录