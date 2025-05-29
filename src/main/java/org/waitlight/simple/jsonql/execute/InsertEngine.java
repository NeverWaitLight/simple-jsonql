package org.waitlight.simple.jsonql.execute;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.waitlight.simple.jsonql.builder.InsertSqlBuilder;
import org.waitlight.simple.jsonql.builder.PreparedSql;
import org.waitlight.simple.jsonql.builder.SqlBuildException;
import org.waitlight.simple.jsonql.execute.result.InsertResult;
import org.waitlight.simple.jsonql.metadata.Metadata;
import org.waitlight.simple.jsonql.metadata.MetadataBuilderFactory;
import org.waitlight.simple.jsonql.metadata.MetadataSource;
import org.waitlight.simple.jsonql.statement.InsertStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 处理插入语句的引擎实现类，负责将InsertStatement转换为SQL并执行
 */
@Slf4j
public class InsertEngine extends StatementEngine<InsertStatement, InsertResult> {

    // Helper record/class for main statement execution result
    private record MainExecutionDetail(Long generatedId, int affectedRows) {
    }

    // Helper record/class for nested statement execution result
    private record NestedExecutionDetail(Long generatedId, int affectedRows) {
    }

    // Helper record/class for aggregated nested statements execution result
    private record NestedExecutionSummary(List<Long> generatedIds, int totalAffectedRows) {
    }

    private final InsertSqlBuilder insertSqlBuilder;

    public InsertEngine(MetadataSource metadataSource) {
        super(metadataSource);
        this.insertSqlBuilder = new InsertSqlBuilder(MetadataBuilderFactory.createLocalBuilder(metadataSource).build());
    }

    public InsertEngine(MetadataSource metadataSource, Metadata metadata) {
        super(metadataSource);
        super.metadata = metadata;
        this.insertSqlBuilder = new InsertSqlBuilder(MetadataBuilderFactory.createLocalBuilder(metadataSource).build());
    }

    @Override
    public InsertResult execute(Connection conn, InsertStatement stmt) throws SQLException, SqlBuildException {
        final PreparedSql<InsertStatement> preparedSql = insertSqlBuilder.build(stmt);

        if (StringUtils.isBlank(preparedSql.getSql()) && CollectionUtils.isEmpty(preparedSql.getNestedSQLs())) {
            return new InsertResult(0, Collections.emptyList(), Collections.emptyList());
        }

        logStatementInfo(stmt, preparedSql);

        boolean originalAutoCommit = conn.getAutoCommit();
        MainExecutionDetail mainResult = null;
        NestedExecutionSummary nestedSummary = new NestedExecutionSummary(new ArrayList<>(), 0);

        try {
            conn.setAutoCommit(false);

            mainResult = executeMainStatement(conn, preparedSql);
            nestedSummary = executeNestedStatements(conn, preparedSql,
                    mainResult != null ? mainResult.generatedId() : null);

            conn.commit();

            InsertResult executionResult = buildInsertResult(mainResult, nestedSummary);
            log.info("事务提交成功，总影响行数: {}, 主ID: {}, 嵌套ID: {}",
                    executionResult.getAffectedRows(), executionResult.getMainIds(), executionResult.getNestedIds());

            return executionResult;
        } catch (SQLException e) {
            handleTransactionError(conn, e);
            InsertResult executionResult = buildInsertResult(mainResult, nestedSummary);
            log.error("事务执行失败，当前状态: {}", executionResult);
            throw e;
        } finally {
            restoreAutoCommit(conn, originalAutoCommit);
        }
    }

    /**
     * 记录SQL语句和相关参数的日志信息
     *
     * @param stmt        插入语句
     * @param preparedSql 预处理SQL对象
     */
    private void logStatementInfo(InsertStatement stmt, PreparedSql<InsertStatement> preparedSql) {
        log.info("执行实体插入语句: {}", stmt.getEntityId());
        if (StringUtils.isNotBlank(preparedSql.getSql())) {
            log.info("主SQL: {}", preparedSql.getSql());
            if (CollectionUtils.isNotEmpty(preparedSql.getParameters())) {
                log.info("主SQL参数: {}", preparedSql.getParameters());
            }
        }
        if (CollectionUtils.isNotEmpty(preparedSql.getNestedSQLs())) {
            log.info("插入语句包含 {} 个嵌套SQL语句", preparedSql.getNestedSQLs().size());
        }
    }

    /**
     * 执行主SQL语句
     *
     * @param conn        数据库连接
     * @param preparedSql 预处理SQL对象
     * @return MainExecutionDetail 包含生成的主键ID和影响行数
     * @throws SQLException 当SQL执行发生错误时抛出
     */
    private MainExecutionDetail executeMainStatement(Connection conn, PreparedSql<InsertStatement> preparedSql)
            throws SQLException {
        if (StringUtils.isBlank(preparedSql.getSql())) {
            return new MainExecutionDetail(null, 0);
        }

        try (PreparedStatement ps = conn.prepareStatement(
                preparedSql.getSql(), PreparedStatement.RETURN_GENERATED_KEYS)) {

            setStatementParameters(ps, preparedSql.getParameters());
            int affected = ps.executeUpdate();
            Long generatedId = extractGeneratedId(ps, affected, "主实体");

            return new MainExecutionDetail(generatedId, affected);
        }
    }

    /**
     * 从执行结果中提取生成的主键ID
     *
     * @param ps               已执行的PreparedStatement
     * @param affected         受影响的行数
     * @param entityTypeForLog 日志中使用的实体类型（如 "主实体", "嵌套实体"）
     * @return 生成的主键ID，如果没有生成则返回null
     * @throws SQLException 当获取生成键时发生错误时抛出
     */
    private Long extractGeneratedId(PreparedStatement ps, int affected, String entityTypeForLog) throws SQLException {
        if (affected <= 0) {
            return null;
        }

        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                Long generatedId = rs.getLong(1);
                log.info("{}生成ID: {}", entityTypeForLog, generatedId);
                return generatedId;
            }
        }
        return null;
    }

    /**
     * 执行所有嵌套SQL语句
     *
     * @param conn            数据库连接
     * @param preparedSql     包含嵌套SQL的预处理SQL对象
     * @param mainGeneratedId 主语句生成的ID (用于外键替换)
     * @return NestedExecutionSummary 包含所有嵌套生成的ID列表和总影响行数
     * @throws SQLException 当SQL执行发生错误时抛出
     */
    private NestedExecutionSummary executeNestedStatements(
            Connection conn, PreparedSql<InsertStatement> preparedSql, Long mainGeneratedId)
            throws SQLException {

        List<Long> allNestedGeneratedIds = new ArrayList<>();
        int totalNestedAffectedRows = 0;

        if (CollectionUtils.isEmpty(preparedSql.getNestedSQLs())) {
            return new NestedExecutionSummary(allNestedGeneratedIds, totalNestedAffectedRows);
        }

        for (PreparedSql<InsertStatement> childSql : preparedSql.getNestedSQLs()) {
            if (StringUtils.isNotBlank(childSql.getSql())) {
                NestedExecutionDetail detail = executeNestedStatement(conn, childSql, mainGeneratedId);
                totalNestedAffectedRows += detail.affectedRows();
                if (detail.generatedId() != null) {
                    allNestedGeneratedIds.add(detail.generatedId());
                }
            }
        }
        return new NestedExecutionSummary(allNestedGeneratedIds, totalNestedAffectedRows);
    }

    /**
     * 执行单个嵌套SQL语句
     *
     * @param conn            数据库连接
     * @param childSql        嵌套SQL对象
     * @param mainGeneratedId 主语句生成的ID (用于外键替换)
     * @return NestedExecutionDetail 包含该嵌套语句生成的主键ID和影响行数
     * @throws SQLException 当SQL执行发生错误时抛出
     */
    private NestedExecutionDetail executeNestedStatement(
            Connection conn, PreparedSql<InsertStatement> childSql, Long mainGeneratedId)
            throws SQLException {

        log.info("执行嵌套SQL: {}", childSql.getSql());
        if (CollectionUtils.isNotEmpty(childSql.getParameters())) {
            log.info("嵌套SQL参数 (ID替换前): {}", childSql.getParameters());
        }

        try (PreparedStatement ps = conn.prepareStatement(childSql.getSql(), PreparedStatement.RETURN_GENERATED_KEYS)) {
            List<Object> params = replaceForeignKeyPlaceholders(childSql.getParameters(), mainGeneratedId);
            setStatementParameters(ps, params);
            log.info("嵌套SQL参数 (ID替换后): {}", params);

            int affected = ps.executeUpdate();
            Long generatedId = extractGeneratedId(ps, affected, "嵌套实体");

            return new NestedExecutionDetail(generatedId, affected);
        }
    }

    private List<Object> replaceForeignKeyPlaceholders(List<Object> parameters, Long generatedId) {
        if (CollectionUtils.isEmpty(parameters)) {
            return Collections.emptyList();
        }

        List<Object> params = new ArrayList<>(parameters);
        for (int j = 0; j < params.size(); j++) {
            Object param = params.get(j);
            if (param != null && InsertSqlBuilder.FOREIGN_KEY_PLACEHOLDER.equals(param.toString())) {
                params.set(j, generatedId);
            }
        }
        return params;
    }

    private void setStatementParameters(PreparedStatement ps, List<Object> parameters) throws SQLException {
        if (CollectionUtils.isEmpty(parameters)) {
            return;
        }
        for (int i = 0; i < parameters.size(); i++) {
            ps.setObject(i + 1, parameters.get(i));
        }
    }

    private void handleTransactionError(Connection conn, SQLException e) {
        try {
            conn.rollback();
            log.error("事务回滚，错误原因: {}", e.getMessage());
        } catch (SQLException rollbackEx) {
            log.error("事务回滚失败", rollbackEx);
        }
    }

    private void restoreAutoCommit(Connection conn, boolean originalAutoCommit) {
        try {
            conn.setAutoCommit(originalAutoCommit);
        } catch (SQLException e) {
            log.error("恢复AutoCommit设置失败", e);
        }
    }

    /**
     * 构建插入结果对象
     */
    private InsertResult buildInsertResult(MainExecutionDetail mainResult, NestedExecutionSummary nestedSummary) {
        int totalAffectedRows = (mainResult != null ? mainResult.affectedRows() : 0)
                + nestedSummary.totalAffectedRows();
        List<Long> mainIds = mainResult != null && mainResult.generatedId() != null
                ? List.of(mainResult.generatedId())
                : Collections.emptyList();
        List<Long> nestedIds = nestedSummary.generatedIds() != null ? nestedSummary.generatedIds()
                : Collections.emptyList();

        return new InsertResult(totalAffectedRows, mainIds, nestedIds);
    }
}