package org.waitlight.simple.jsonql.metadata;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.waitlight.simple.jsonql.util.JDBCTypeUtils;

import java.sql.JDBCType;
import java.util.Objects;

public class JOOQMetadataCache implements MetadataCache<PersistentClass, Table<?>> {

    private final Schema schema = DSL.schema("default");

    @Override
    public void add(PersistentClass persistentClass) {
        Table<Record> table = DSL.table(persistentClass.getTableName(), schema);

        for (Property property : persistentClass.getProperties()) {
            String columnName = property.columnName();
            JDBCType columnType = property.columnType();
            Field<?> field = DSL.field(columnName, JDBCTypeUtils.getDataType(columnType));
            table.field(field);
        }
    }

    @Override
    public Table<?> get(String entityName) {
        Table<?> table = schema.getTable(entityName);
        if (Objects.isNull(table)) {
            throw new MetadataException("Table not found: '" + entityName + "'");
        }
        return table;
    }
}