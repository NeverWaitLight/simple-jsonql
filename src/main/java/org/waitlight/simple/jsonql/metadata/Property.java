package org.waitlight.simple.jsonql.metadata;

import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Property {
    private String name;
    private String column;
    private String type;
    private RelationshipType relationshipType; // 关系类型（一对一、一对多、多对一、多对多）
    private Class<?> targetEntity; // 关联的目标实体类
    private String mappedBy; // 双向关系的映射字段
    private String foreignKeyName; // 外键约束名称
    private boolean nullable; // 字段是否可为空
    private String joinTableName; // 多对多关系的关联表名
    private JoinColumn[] joinColumns; // 多对多关系的主表连接列
    private JoinColumn[] inverseJoinColumns; // 多对多关系的从表连接列

    public Property(String name, String column, String type) {
        this.name = name;
        this.column = column;
        this.type = type;
    }
}
