package org.waitlight.simple.jsonql.metadata;

import jakarta.persistence.JoinColumn;

import java.sql.JDBCType;

public record Property(
        String fieldName,
        Class<?> fieldType,

        String columnName,
        JDBCType columnType,

        RelationshipType relationship,      // 关系类型
        Class<?> targetEntity,              // 关联的目标实体类
        String mappedBy,                    // 双向关系的映射字段
        String foreignKeyName,              // 外键约束名称
        String joinTableName,               // 多对多关系的关联表名
        JoinColumn[] joinColumns,           // 多对多关系的主表连接列
        JoinColumn[] inverseJoinColumns     // 多对多关系的从表连接列
) {
    public static class Builder {
        private String fieldName;
        private Class<?> fieldType;
        private String columnName;
        private JDBCType columnType;
        private RelationshipType relationship;
        private Class<?> targetEntity;
        private String mappedBy;
        private String foreignKeyName;
        private String joinTableName;
        private JoinColumn[] joinColumns;
        private JoinColumn[] inverseJoinColumns;

        public Builder() {
        }

        public Builder(Property property) {
            if (property != null) {
                this.fieldName = property.fieldName();
                this.fieldType = property.fieldType();
                this.columnName = property.columnName();
                this.columnType = property.columnType();
                this.relationship = property.relationship();
                this.targetEntity = property.targetEntity();
                this.mappedBy = property.mappedBy();
                this.foreignKeyName = property.foreignKeyName();
                this.joinTableName = property.joinTableName();
                this.joinColumns = property.joinColumns();
                this.inverseJoinColumns = property.inverseJoinColumns();
            }
        }

        public Builder setFieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        public Builder setFieldType(Class<?> fieldType) {
            this.fieldType = fieldType;
            return this;
        }

        public Builder setColumnName(String columnName) {
            this.columnName = columnName;
            return this;
        }

        public Builder setColumnType(JDBCType columnType) {
            this.columnType = columnType;
            return this;
        }

        public Builder setRelationship(RelationshipType relationship) {
            this.relationship = relationship;
            return this;
        }

        public Builder setTargetEntity(Class<?> targetEntity) {
            this.targetEntity = targetEntity;
            return this;
        }

        public Builder setMappedBy(String mappedBy) {
            this.mappedBy = mappedBy;
            return this;
        }

        public Builder setForeignKeyName(String foreignKeyName) {
            this.foreignKeyName = foreignKeyName;
            return this;
        }

        public Builder setJoinTableName(String joinTableName) {
            this.joinTableName = joinTableName;
            return this;
        }

        public Builder setJoinColumns(JoinColumn[] joinColumns) {
            this.joinColumns = joinColumns;
            return this;
        }

        public Builder setInverseJoinColumns(JoinColumn[] inverseJoinColumns) {
            this.inverseJoinColumns = inverseJoinColumns;
            return this;
        }

        public Property build() {
            return new Property(
                    fieldName,
                    fieldType,
                    columnName,
                    columnType,
                    relationship,
                    targetEntity,
                    mappedBy,
                    foreignKeyName,
                    joinTableName,
                    joinColumns,
                    inverseJoinColumns);
        }
    }
}
