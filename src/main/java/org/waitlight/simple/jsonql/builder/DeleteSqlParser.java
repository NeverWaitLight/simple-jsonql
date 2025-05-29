package org.waitlight.simple.jsonql.builder;

import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.waitlight.simple.jsonql.metadata.Metadata;
import org.waitlight.simple.jsonql.statement.DeleteStatement;

import java.util.ArrayList;
import java.util.List;

public class DeleteSqlParser extends AbstractSqlBuilder<DeleteStatement> {

    private static final Logger log = LoggerFactory.getLogger(DeleteSqlParser.class);

    public DeleteSqlParser(Metadata metadata) {
        super(metadata);
    }

    @Override
    public PreparedSql<DeleteStatement> build(DeleteStatement statement) throws SqlBuildException {
        if (statement == null) {
            throw new SqlBuildException("DeleteStatement is null");
        }

        String entityId = statement.getEntityId();
        if (StringUtils.isBlank(entityId)) {
            throw new SqlBuildException("entityId is required for delete statements");
        }

        // 只支持单个ID删除，检查id字段
        String id = statement.getId();
        if (StringUtils.isBlank(id)) {
            throw new SqlBuildException("id is required for delete statement");
        }

        return buildSql(entityId, id);
    }

    /**
     * 构建DELETE SQL语句及其参数
     */
    private PreparedSql<DeleteStatement> buildSql(String entityId, String id) throws SqlBuildException {
        DSLContext create = DSL.using(SQLDialect.MYSQL);
        Table<?> table = DSL.table(DSL.name(entityId));

        List<Object> parameters = new ArrayList<>();

        // 使用jooq构建DELETE语句
        String sql = create.deleteFrom(table)
                .where(DSL.field(DSL.name("id")).eq(DSL.param()))
                .getSQL();

        parameters.add(id);

        log.info("build delete sql: {}", sql);

        PreparedSql<DeleteStatement> preparedSql = new PreparedSql<>();
        preparedSql.setSql(sql);
        preparedSql.setParameters(parameters);
        preparedSql.setStatementType(DeleteStatement.class);

        return preparedSql;
    }
}