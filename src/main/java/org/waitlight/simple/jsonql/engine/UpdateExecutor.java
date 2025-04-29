package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.UpdateStatement;
import org.waitlight.simple.jsonql.statement.model.*;
import org.waitlight.simple.jsonql.statement.model.Field;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UpdateExecutor extends StatementExecutor {
    private static UpdateExecutor instance;
    private final WhereClauseExecutor whereClauseExecutor;

    private UpdateExecutor(MetadataSources metadataSources) {
        super(metadataSources);
        this.whereClauseExecutor = new WhereClauseExecutor(metadataSources);
    }

    public static synchronized UpdateExecutor getInstance(MetadataSources metadataSources) {
        if (instance == null) {
            instance = new UpdateExecutor(metadataSources);
        }
        return instance;
    }

    @Override
    protected Object doExecute(Connection conn, SqlAndParameters sqlAndParameters) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sqlAndParameters.sql())) {
            return stmt.executeUpdate();
        }
    }

    @Override
    protected SqlAndParameters parseSql(JsonQLStatement statement) {
        if (!(statement instanceof UpdateStatement)) {
            throw new IllegalArgumentException("Expected UpdateStatement but got " + statement.getClass().getSimpleName());
        }
        UpdateStatement updateStatement = (UpdateStatement) statement;
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ")
                .append(updateStatement.getEntityId())
                .append(" SET ");

        // 处理 SET 子句
        List<String> setClauses = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        
        for (Field field : updateStatement.getFields()) {
            if (field.getValues() != null) {
                // 处理嵌套实体
                log.warn("Nested entity updates are not supported in SQL: {}", field.getField());
                continue;
            }
            setClauses.add(field.getField() + " = ?");
            parameters.add(field.getValue());
        }
        
        sql.append(String.join(", ", setClauses));

        // 添加 WHERE 子句
        sql.append(" WHERE id = ?");
        parameters.add(updateStatement.getDataId());

        return new SqlAndParameters(sql.toString(), parameters);
    }
}
