package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.waitlight.simple.jsonql.metadata.Metadata;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.JsonqlStatement;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * SQL 语句执行器抽象类
 * 用于处理不同类型的 SQL 语句执行
 */
@Slf4j
public abstract class StatementExecutor {
    protected final Metadata metadata;
    protected final MetadataSources metadataSources;

    protected StatementExecutor(MetadataSources metadataSources) {
        this.metadataSources = metadataSources;
        this.metadata = metadataSources.buildMetadata();
    }

    public Object execute(Connection conn, JsonqlStatement statement) throws SQLException {
        SqlAndParameters sqlAndParameters = parseSql(statement);
        log.info("SQL: {}", sqlAndParameters.sql());
        if (CollectionUtils.isNotEmpty(sqlAndParameters.parameters())) {
            log.info("Parameters: {}", sqlAndParameters.parameters());
        }
        return doExecute(conn, sqlAndParameters);
    }

    /**
     * 执行 SQL 语句
     */
    protected abstract Object doExecute(Connection conn, SqlAndParameters sql) throws SQLException;

    /**
     * 解析 SQL 语句
     *
     * @param statement SQL 语句对象
     * @return 解析后的 SQL 语句
     */
    protected abstract SqlAndParameters parseSql(JsonqlStatement statement);

    public record SqlAndParameters(
            String sql,
            List<Object> parameters
    ) {
    }
} 