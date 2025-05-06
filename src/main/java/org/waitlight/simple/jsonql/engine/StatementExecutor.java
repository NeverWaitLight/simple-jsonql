package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.waitlight.simple.jsonql.metadata.Metadata;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.CreateStatement;
import org.waitlight.simple.jsonql.statement.model.JsonQLStatement;
// import org.waitlight.simple.jsonql.statement.model.QueryStatement; // 暂时注释掉，如果需要再引入

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // 重命名内部记录类以避免与 java.sql.Statement 冲突，并添加泛型支持
    public record PreparedSql<T extends JsonQLStatement>(
            String sql,
            List<Object> parameters,
            // 用于存储嵌套的 CreateStatement
            List<CreateStatement> nestedCreateStatements,
            // 可以添加其他类型的嵌套语句，例如嵌套查询
            // List<QueryStatement> nestedQueryStatements // 示例
            Class<T> statementType // 记录原始语句类型
    ) {
        // 紧凑型构造函数用于初始化/验证
        public PreparedSql {
            // 确保 nestedCreateStatements 列表永远不为 null
            if (nestedCreateStatements == null) {
                nestedCreateStatements = new ArrayList<>();
            }
            // 可以在这里添加其他验证逻辑
        }

        // 便利构造函数 - 无嵌套语句
        public PreparedSql(String sql, List<Object> parameters, Class<T> statementType) {
            this(sql, parameters, new ArrayList<>(), statementType);
        }
        
        // 便利构造函数 - 包含嵌套创建语句 (如果需要，可以保留，但紧凑型已处理null)
        // public PreparedSql(String sql, List<Object> parameters, List<CreateStatement> nestedCreateStatements, Class<T> statementType) {
        //     this(sql, parameters, nestedCreateStatements, statementType); 
        // }

        // 获取嵌套实体的总数
        public int getNestedEntityCount() {
            return nestedCreateStatements.size(); // 因为构造函数保证非null
        }
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