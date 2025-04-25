package org.waitlight.simple.jsonql.claude;

import java.util.HashMap;
import java.util.Map;

/**
 * 实体元数据，存储实体类的表映射信息
 */
public class EntityMetadata {
    private final String tableName;
    private final String primaryKey;
    private final Map<String, ColumnInfo> columns = new HashMap<>();
    private final Map<String, RelationshipInfo> relationships = new HashMap<>();
    
    public EntityMetadata(String tableName, String primaryKey) {
        this.tableName = tableName;
        this.primaryKey = primaryKey;
    }

    public String getTableName() {
        return tableName;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public Map<String, ColumnInfo> getColumns() {
        return columns;
    }

    public Map<String, RelationshipInfo> getRelationships() {
        return relationships;
    }
}

/**
 * 列信息
 */
class ColumnInfo {
    private final String columnName;
    private final String dataType;
    
    public ColumnInfo(String columnName, String dataType) {
        this.columnName = columnName;
        this.dataType = dataType;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getDataType() {
        return dataType;
    }
}

/**
 * 关系信息
 */
class RelationshipInfo {
    private final String type; // "OneToMany", "ManyToOne", etc.
    private final Class<?> targetEntity;
    private final String foreignKey;
    
    public RelationshipInfo(String type, Class<?> targetEntity, String foreignKey) {
        this.type = type;
        this.targetEntity = targetEntity;
        this.foreignKey = foreignKey;
    }

    public String getType() {
        return type;
    }

    public Class<?> getTargetEntity() {
        return targetEntity;
    }

    public String getForeignKey() {
        return foreignKey;
    }
}