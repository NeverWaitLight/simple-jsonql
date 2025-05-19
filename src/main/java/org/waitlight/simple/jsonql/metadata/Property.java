package org.waitlight.simple.jsonql.metadata;

import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Property {
    private String fieldName;
    private String columnName;
    private String type;

    /**
     * 关系类型（一对一、一对多、多对一、多对多）
     */
    private RelationshipType relationship;

    /**
     * 关联的目标实体类
     */
    private Class<?> targetEntity;

    /**
     * 双向关系的映射字段
     */
    private String mappedBy;

    /**
     * 外键约束名称
     */
    private String foreignKeyName;

    /**
     * 字段是否可为空
     */
    private boolean nullable;

    /**
     * 多对多关系的关联表名
     */
    private String joinTableName;

    /**
     * 多对多关系的主表连接列
     */
    private JoinColumn[] joinColumns;

    /**
     * 多对多关系的从表连接列
     */
    private JoinColumn[] inverseJoinColumns;

    public Property(String fieldName, String columnName, String type) {
        this.fieldName = fieldName;
        this.columnName = columnName;
        this.type = type;
    }
}
