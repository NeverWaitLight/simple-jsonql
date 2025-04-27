package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.JsonqlStatement;
import org.waitlight.simple.jsonql.statement.model.UpdateStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
public class UpdateExecutor extends StatementExecutor {
    private static UpdateExecutor instance;

    private UpdateExecutor(MetadataSources metadataSources) {
        super(metadataSources);
    }

    public static synchronized UpdateExecutor getInstance(MetadataSources metadataSources) {
        if (instance == null) {
            instance = new UpdateExecutor(metadataSources);
        }
        return instance;
    }

    @Override
    protected Object doExecute(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            return stmt.executeUpdate();
        }
    }

    @Override
    protected String parseSql(JsonqlStatement statement) {
        if (!(statement instanceof UpdateStatement)) {
            throw new IllegalArgumentException("Expected UpdateStatement but got " + statement.getClass().getSimpleName());
        }
        UpdateStatement updateStatement = (UpdateStatement) statement;
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ")
                .append(updateStatement.getUpdate())
                .append(" SET ");

        // 处理 SET 子句
        String[] setClauses = updateStatement.getSet().entrySet().stream()
                .map(entry -> entry.getKey() + " = ?")
                .toArray(String[]::new);
        sql.append(String.join(", ", setClauses));

        // 处理 WHERE 子句
        if (updateStatement.getWhere() != null) {
            sql.append(" WHERE ")
                    .append(updateStatement.getWhere().toString());
        }

        return sql.toString();
    }
} 