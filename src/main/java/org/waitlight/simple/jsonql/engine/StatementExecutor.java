package org.waitlight.simple.jsonql.engine;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.collections4.CollectionUtils;
import org.waitlight.simple.jsonql.metadata.Metadata;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;

import lombok.extern.slf4j.Slf4j;

/**
 * SQL 语句执行器抽象类
 * 用于处理不同类型的 SQL 语句执行
 */
@Slf4j
public abstract class StatementExecutor<T extends JsonQLStatement> {
    protected final Metadata metadata;
    protected final MetadataSources metadataSources;

    protected StatementExecutor(MetadataSources metadataSources) {
        this.metadataSources = metadataSources;
        this.metadata = metadataSources.buildMetadata();
    }

    // execute 方法签名调整
    public Object execute(Connection conn, JsonQLStatement statement) throws SQLException {
        PreparedSql<T> preparedSql = parseSql(statement);
        int totalAffectedRows = 0;
        
        // 记录主SQL
        log.info("SQL: {}", preparedSql.getSql());
        if (CollectionUtils.isNotEmpty(preparedSql.getParameters())) {
            log.info("Parameters: {}", preparedSql.getParameters());
        }
        
        // 执行主SQL
        Object result = doExecute(conn, preparedSql);
        if (result instanceof Integer) {
            totalAffectedRows += (Integer) result;
        }
        
        // 执行嵌套SQL
        if (CollectionUtils.isNotEmpty(preparedSql.getNestedSQLs())) {
            for (PreparedSql<?> nestedSql : preparedSql.getNestedSQLs()) {
                totalAffectedRows += executeNestedSql(conn, nestedSql);
            }
        }
        
        return totalAffectedRows;
    }
    
    /**
     * 递归执行嵌套SQL
     * 
     * @param conn 数据库连接
     * @param sql 要执行的SQL
     * @return 受影响的行数
     */
    private int executeNestedSql(Connection conn, PreparedSql<?> sql) throws SQLException {
        int affectedRows = 0;
        
        // 记录SQL
        log.info("Nested SQL: {}", sql.getSql());
        if (CollectionUtils.isNotEmpty(sql.getParameters())) {
            log.info("Nested Parameters: {}", sql.getParameters());
        }
        
        // 执行SQL
        Object result = doExecute(conn, sql);
        if (result instanceof Integer) {
            affectedRows += (Integer) result;
        }
        
        // 递归执行更深层嵌套SQL
        if (CollectionUtils.isNotEmpty(sql.getNestedSQLs())) {
            for (PreparedSql<?> nestedSql : sql.getNestedSQLs()) {
                affectedRows += executeNestedSql(conn, nestedSql);
            }
        }
        
        return affectedRows;
    }

    /**
     * 执行 SQL 语句
     */
    protected abstract Object doExecute(Connection conn, PreparedSql<?> sql) throws SQLException;

    /**
     * 解析 SQL 语句
     *
     * @param statement SQL 语句对象
     * @return 解析后的 SQL 语句对象，其中嵌套子SQL保存在nestedSqls字段
     */
    protected abstract PreparedSql<T> parseSql(JsonQLStatement statement);
} 