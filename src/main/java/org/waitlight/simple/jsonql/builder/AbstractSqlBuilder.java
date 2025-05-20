package org.waitlight.simple.jsonql.builder;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.waitlight.simple.jsonql.metadata.Metadata;
import org.waitlight.simple.jsonql.metadata.Property;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;
import org.waitlight.simple.jsonql.statement.model.FieldStatement;

import java.util.Map;
import java.util.Objects;

public abstract class AbstractSqlBuilder<T extends JsonQLStatement> implements SqlBuilder<T> {

    protected final Metadata metadata;

    /**
     * SQL 方言
     */
    private final SqlDialect dialect = SqlDialect.DatabaseProduct.MYSQL.getDialect();

    /**
     * SQL 转换器
     */
    private final RelToSqlConverter converter = new RelToSqlConverter(dialect);


    public AbstractSqlBuilder(Metadata metadata) {
        this.metadata = metadata;
    }

    protected Map<FieldStatement, Property> map(String entityName, JsonQLStatement statement) {
        return null;
    }

    protected String build(RelNode relNode) throws SqlBuildeException {
        if (Objects.isNull(relNode)) {
            throw new SqlBuildeException("RelNode is null");
        }

        SqlNode sqlNode = converter.visitRoot(relNode).asStatement();
        return sqlNode.toSqlString(dialect).getSql();
    }
}
