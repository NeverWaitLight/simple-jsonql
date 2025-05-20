package org.waitlight.simple.jsonql.execute;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.waitlight.simple.jsonql.execute.result.DeleteResult;
import org.waitlight.simple.jsonql.builder.DeleteSqlParser;
import org.waitlight.simple.jsonql.builder.PreparedSql;
import org.waitlight.simple.jsonql.metadata.MetadataSource;
import org.waitlight.simple.jsonql.statement.DeleteStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Slf4j
public class DeleteEngine extends StatementEngine<DeleteStatement, DeleteResult> {
    private final DeleteSqlParser deleteSqlParser;

    public DeleteEngine(MetadataSource metadataSource) {
        super(metadataSource);
        this.deleteSqlParser = new DeleteSqlParser();
    }

    @Override
    public DeleteResult execute(Connection conn, DeleteStatement stmt) throws SQLException {
        PreparedSql<DeleteStatement> preparedSql = deleteSqlParser.parseSql(stmt);

        if (preparedSql.getSql() == null || preparedSql.getSql().isEmpty()) {
            log.info("生成的SQL为空，不执行删除操作。 Entity: {}", stmt.getEntityId());
            return DeleteResult.of(0); // No SQL to execute
        }

        log.info("执行删除语句 Entity: {}", stmt.getEntityId());
        log.info("主SQL: {}", preparedSql.getSql());
        if (CollectionUtils.isNotEmpty(preparedSql.getParameters())) {
            log.info("主SQL参数: {}", preparedSql.getParameters());
        }

        try (PreparedStatement ps = conn.prepareStatement(preparedSql.getSql())) {
            List<Object> parameters = preparedSql.getParameters();
            if (CollectionUtils.isNotEmpty(parameters)) {
                for (int i = 0; i < parameters.size(); i++) {
                    ps.setObject(i + 1, parameters.get(i));
                }
            }
            int affectedRows = ps.executeUpdate();
            log.info("删除操作影响行数: {}", affectedRows);
            return DeleteResult.of(affectedRows);
        } catch (SQLException e) {
            log.error("删除操作执行失败 Entity: {}. SQL: {}. Error: {}", stmt.getEntityId(), preparedSql.getSql(),
                    e.getMessage());
            throw e;
        }
    }
}
