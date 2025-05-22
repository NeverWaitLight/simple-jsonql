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

public abstract class AbstractSqlBuilder<T extends JsonQLStatement> {

    public static final String FOREIGN_KEY_PLACEHOLDER = "__FOREIGN_KEY_PLACEHOLDER__";

    protected final Metadata metadata;

    private final SqlDialect dialect = SqlDialect.DatabaseProduct.MYSQL.getDialect();   // SQL 方言
    private final RelToSqlConverter converter = new RelToSqlConverter(dialect);         // SQL 转换器


    public AbstractSqlBuilder(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * 通过 {@link JsonQLStatement} 构建 sql
     *
     * @param statement 待处理的语句
     * @return 预处理后的 sql
     * @throws SqlBuildException 构建异常
     */
    protected abstract PreparedSql<T> build(T statement) throws SqlBuildException;

    protected abstract Map<FieldStatement, Property> map(String entityName, T statement);

    /**
     * Base on apache calcite
     */
    protected String build(RelNode relNode) throws SqlBuildException {
        if (Objects.isNull(relNode)) {
            throw new SqlBuildException("RelNode is null");
        }

        SqlNode sqlNode = converter.visitRoot(relNode).asStatement();
        return sqlNode.toSqlString(dialect).getSql();
    }
}
