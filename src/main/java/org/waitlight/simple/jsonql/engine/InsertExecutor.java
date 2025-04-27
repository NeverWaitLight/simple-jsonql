package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.InsertStatement;
import org.waitlight.simple.jsonql.statement.model.JsonqlStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
public class InsertExecutor extends StatementExecutor {
    private static InsertExecutor instance;

    private InsertExecutor(MetadataSources metadataSources) {
        super(metadataSources);
    }

    public static synchronized InsertExecutor getInstance(MetadataSources metadataSources) {
        if (instance == null) {
            instance = new InsertExecutor(metadataSources);
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
        if (!(statement instanceof InsertStatement)) {
            throw new IllegalArgumentException("Expected InsertStatement but got " + statement.getClass().getSimpleName());
        }
        InsertStatement insertStatement = (InsertStatement) statement;
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ")
                .append(insertStatement.getInto())
                .append(" (");

        // 处理字段
        String[] fields = insertStatement.getValues().keySet().toArray(new String[0]);
        sql.append(String.join(", ", fields));

        sql.append(") VALUES (");

        // 处理值
        String[] placeholders = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            placeholders[i] = "?";
        }
        sql.append(String.join(", ", placeholders));

        sql.append(")");

        return sql.toString();
    }
} 