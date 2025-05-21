package org.waitlight.simple.jsonql.metadata;

import lombok.Getter;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;

import java.sql.JDBCType;
import java.util.Objects;

public class CalciteMetadataCache implements MetadataCache<PersistentClass, Table> {

    private final SchemaPlus schemaPlus;
    @Getter
    private final FrameworkConfig frameworkConfig;

    public CalciteMetadataCache() {
        this.schemaPlus = CalciteSchema.createRootSchema(true, true).plus();
        this.frameworkConfig = Frameworks.newConfigBuilder()
                .defaultSchema(schemaPlus)
                .build();
    }

    @Override
    public void add(PersistentClass persistentClass) {
        String tableName = persistentClass.getTableName();
        if (Objects.nonNull(schemaPlus.tables().get(tableName))) {
            throw new MetadataException("Duplicate table name: '" + tableName + "'");
        }

        RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
        RelDataTypeFactory.Builder builder = typeFactory.builder();

        for (Property property : persistentClass.getProperties()) {
            String columnName = property.columnName();
            JDBCType columnType = property.columnType();
            SqlTypeName sqlTypeName = SqlTypeName.getNameForJdbcType(columnType.getVendorTypeNumber());
            if (sqlTypeName != null) {
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
        schemaPlus.add(tableName, table);
    }

    @Override
    public Table get(String entityName) {
        return schemaPlus.tables().get(entityName);
    }
}
