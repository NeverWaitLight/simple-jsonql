package org.waitlight.simple.jsonql.engine;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.collections4.CollectionUtils;
import org.waitlight.simple.jsonql.metadata.Metadata;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.JsonQLStatement;

import lombok.extern.slf4j.Slf4j;

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

    // execute 方法签名调整
    public Object execute(Connection conn, JsonQLStatement statement) throws SQLException {
        PreparedSql<?> preparedSql = parseSql(statement);
        log.info("SQL: {}", preparedSql.sql());
        if (CollectionUtils.isNotEmpty(preparedSql.parameters())) {
            log.info("Parameters: {}", preparedSql.parameters());
        }
        return doExecute(conn, preparedSql);
    }

    /**
     * 执行 SQL 语句
     */
    protected abstract Object doExecute(Connection conn, PreparedSql<?> sql) throws SQLException;

    /**
     * 解析 SQL 语句
     *
     * @param statement SQL 语句对象
     * @return 解析后的 SQL 语句
     */
    protected abstract PreparedSql<?> parseSql(JsonQLStatement statement);

} 