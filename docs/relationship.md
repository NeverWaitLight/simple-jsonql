以下是修改后的完整答案，将 `@OneToOne` 的共享主键实现改为常规外键方式：

### 一、`@ManyToOne` 和 `@OneToMany`（一对多/多对一）

#### 1. **双向关系**

- **场景**：订单（Order）和订单项（OrderItem）
  - 一个订单有多个订单项（`Order` → `OrderItem`：`@OneToMany`）
  - 一个订单项属于一个订单（`OrderItem` → `Order`：`@ManyToOne`）

**表结构**：

```sql
CREATE TABLE order (
    id INT PRIMARY KEY,
    order_number VARCHAR(255)
);

CREATE TABLE order_item (
    id INT PRIMARY KEY,
    product_name VARCHAR(255),
    order_id INT,  -- 外键指向 order.id
    FOREIGN KEY (order_id) REFERENCES order(id)
);
```

**实体映射**：

```java
// Order 实体
@Entity
public class Order {
    @Id @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "order")  // 由 OrderItem.order 维护关系
    private List<OrderItem> items;
}

// OrderItem 实体
@Entity
public class OrderItem {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")  // 外键列名
    private Order order;
}
```

#### 2. **单向关系**

- **场景**：仅从订单项（OrderItem）访问订单（Order），不反向访问。

**实体映射**：

```java
// Order 实体（无关联属性）
@Entity
public class Order {
    @Id @GeneratedValue
    private Long id;
}

// OrderItem 实体
@Entity
public class OrderItem {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
```

### 二、`@OneToOne`（一对一）

#### 1. **双向关系（常规外键方式）**

- **场景**：用户（User）和用户资料（UserProfile）
  - 用户资料表通过外键关联用户表，**不使用共享主键**。

**表结构**：

```sql
CREATE TABLE user (
    id INT PRIMARY KEY,
    username VARCHAR(255)
);

CREATE TABLE user_profile (
    id INT PRIMARY KEY,
    address VARCHAR(255),
    user_id INT UNIQUE,  -- 外键指向 user.id，唯一约束保证一对一
    FOREIGN KEY (user_id) REFERENCES user(id)
);
```

**实体映射**：

```java
// User 实体
@Entity
public class User {
    @Id @GeneratedValue
    private Long id;

    @OneToOne(mappedBy = "user")  // 由 UserProfile.user 维护关系
    private UserProfile profile;
}

// UserProfile 实体
@Entity
public class UserProfile {
    @Id @GeneratedValue
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)  // 外键列名，唯一约束
    private User user;
}
```

#### 2. **单向关系**

- **场景**：仅从用户资料（UserProfile）访问用户（User）。

**实体映射**：

```java
// User 实体（无关联属性）
@Entity
public class User {
    @Id @GeneratedValue
    private Long id;
}

// UserProfile 实体
@Entity
public class UserProfile {
    @Id @GeneratedValue
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}
```

### 三、`@ManyToMany`（多对多）

#### 1. **双向关系**

- **场景**：学生（Student）和课程（Course）
  - 一个学生可以选多门课程，一门课程可以被多个学生选。

**表结构**：

```sql
CREATE TABLE student (
    id INT PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE course (
    id INT PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE student_course (  -- 中间表
    student_id INT,
    course_id INT,
    PRIMARY KEY (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES student(id),
    FOREIGN KEY (course_id) REFERENCES course(id)
);
```

**实体映射**：

```java
// Student 实体
@Entity
public class Student {
    @Id @GeneratedValue
    private Long id;

    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> courses;
}

// Course 实体
@Entity
public class Course {
    @Id @GeneratedValue
    private Long id;

    @ManyToMany(mappedBy = "courses")  // 由 Student.courses 维护关系
    private List<Student> students;
}
```

#### 2. **单向关系**

- **场景**：仅从学生（Student）访问课程（Course）。

**实体映射**：

```java
// Student 实体
@Entity
public class Student {
    @Id @GeneratedValue
    private Long id;

    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> courses;
}

// Course 实体（无关联属性）
@Entity
public class Course {
    @Id @GeneratedValue
    private Long id;
}
```

### 关键总结

| 关系类型      | 数据库实现                 | 单向 vs 双向                      |
| ------------- | -------------------------- | --------------------------------- |
| `@OneToMany`  | 外键在多的一方（或中间表） | 双向需 `mappedBy`，单向仅单方定义 |
| `@ManyToOne`  | 外键在多的一方             | 通常与 `@OneToMany` 配合实现双向  |
| `@OneToOne`   | **独立外键列**（唯一约束） | 双向需指定维护方                  |
| `@ManyToMany` | 中间表存储关联关系         | 双向需 `mappedBy`，单向仅单方定义 |

#### 补充说明

- **`@OneToOne` 常规实现**：
  - 通过独立的外键列（如 `user_id`）关联目标表，并添加 `unique` 约束保证一对一。
  - 不再使用共享主键（`@MapsId`），适合需要独立生成主键的场景。
- **维护方**：
  - 双向关系中，通过 `mappedBy` 指定非维护方（如 `User.profile` 由 `UserProfile.user` 维护）。
- **外键控制**：
  - 使用 `@JoinColumn(name = "列名")` 显式指定外键字段。
