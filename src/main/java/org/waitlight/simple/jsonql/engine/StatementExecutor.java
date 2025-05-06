package org.waitlight.simple.jsonql.engine;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

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
public abstract class StatementExecutor<T extends JsonQLStatement> {
    protected final Metadata metadata;
    protected final MetadataSources metadataSources;

    protected StatementExecutor(MetadataSources metadataSources) {
        this.metadataSources = metadataSources;
        this.metadata = metadataSources.buildMetadata();
    }

    // execute 方法签名调整
    public Object execute(Connection conn, JsonQLStatement statement) throws SQLException {
        List<PreparedSql<T>> preparedSqls = parseSql(statement);
        int totalAffectedRows = 0;
        
        for (PreparedSql<?> preparedSql : preparedSqls) {
            log.info("SQL: {}", preparedSql.getSql());
            if (CollectionUtils.isNotEmpty(preparedSql.getParameters())) {
                log.info("Parameters: {}", preparedSql.getParameters());
            }
            Object result = doExecute(conn, preparedSql);
            if (result instanceof Integer) {
                totalAffectedRows += (Integer) result;
            }
        }
        
        return totalAffectedRows;
    }

    /**
     * 执行 SQL 语句
     */
    protected abstract Object doExecute(Connection conn, PreparedSql<?> sql) throws SQLException;

    /**
     * 解析 SQL 语句
     *
     * @param statement SQL 语句对象
     * @return 解析后的 SQL 语句列表
     */
    protected abstract List<PreparedSql<T>> parseSql(JsonQLStatement statement);
} 