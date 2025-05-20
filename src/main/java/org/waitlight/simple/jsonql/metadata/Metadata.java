package org.waitlight.simple.jsonql.metadata;

import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.commons.lang3.StringUtils;

import java.sql.JDBCType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Metadata {
    private final Map<String, PersistentClass> entities = new HashMap<>();
    private final SchemaPlus schema;

    public Metadata() {
        this.schema = CalciteSchema.createRootSchema(true, true).plus();
    }

    /**
     * 添加实体类到元数据中，并将其注册到 Calcite Schema
     *
     * @param persistentClass 持久化类元数据信息
     * @throws MetadataException 如果实体名称重复则抛出异常
     */
    public void add(PersistentClass persistentClass) {
        String entityName = persistentClass.getEntityName();
        if (entities.containsKey(entityName)) {
            throw new MetadataException("Duplicate entity name: '" + entityName + "'");
        }
        entities.put(entityName, persistentClass);
        add2Schema(persistentClass);
    }

    public PersistentClass getEntity(String entityName) {
        if (StringUtils.isBlank(entityName)) {
            throw new RuntimeException("Entity not found");
        }
        return Optional.ofNullable(entities.get(entityName))
                .orElse(entities.get(entityName.toLowerCase()));
    }


    /**
     * 将持久化类元数据注册到 Calcite Schema 中
     *
     * @param persistentClass 持久化类元数据信息
     * @throws MetadataException 如果表名重复则抛出异常
     */
    private void add2Schema(PersistentClass persistentClass) {
        String tableName = persistentClass.getTableName();
        if (Objects.nonNull(schema.getTable(tableName))) {
            throw new MetadataException("Duplicate table name: '" + tableName + "'");
        }

        RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
        RelDataTypeFactory.Builder builder = typeFactory.builder();

        for (Property property : persistentClass.getProperties()) {
            String columnName = property.getColumnName();
            JDBCType columnType = property.getColumnType();
            SqlTypeName sqlTypeName = SqlTypeName.getNameForJdbcType(columnType.getVendorTypeNumber());
            if (Objects.nonNull(sqlTypeName)) {
                builder.add(columnName, sqlTypeName);
            }
        }

        RelDataType relDataType = builder.build();
        Table table = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return relDataType;
            }
        };
        schema.add(tableName, table);
    }
}
