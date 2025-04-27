package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.metadata.Metadata;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.JsonqlStatement;

import java.sql.Connection;
import java.sql.SQLException;

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
        String sql = parseSql(statement);
        log.info(sql);
        return doExecute(conn, sql);
    }

    /**
     * 执行 SQL 语句
     *
     * @param conn 数据库连接
     * @param sql  SQL 语句
     * @return 执行结果
     * @throws SQLException 如果执行过程中发生 SQL 异常
     */
    protected abstract Object doExecute(Connection conn, String sql) throws SQLException;

    /**
     * 解析 SQL 语句
     *
     * @param statement SQL 语句对象
     * @return 解析后的 SQL 语句
     */
    protected abstract String parseSql(JsonqlStatement statement);
} 