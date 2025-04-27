package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.InsertStatement;
import org.waitlight.simple.jsonql.statement.model.JsonqlStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
    protected Object doExecute(Connection conn, SqlAndParameters sqlAndParameters) throws SQLException {
        try (PreparedStatement preparedStatement = conn.prepareStatement(sqlAndParameters.sql())) {
            for (int i = 0; i < sqlAndParameters.parameters().size(); i++) {
                preparedStatement.setObject(i + 1, sqlAndParameters.parameters().get(i));
            }
            return preparedStatement.executeUpdate();
        }
    }

    @Override
    protected SqlAndParameters parseSql(JsonqlStatement statement) {
        if (!(statement instanceof InsertStatement)) {
            throw new IllegalArgumentException("Expected InsertStatement but got " + statement.getClass().getSimpleName());
        }
        InsertStatement insertStatement = (InsertStatement) statement;
        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
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
        List<Object> values = new ArrayList<>(insertStatement.getValues().values());
        for (int i = 0; i < values.size(); i++) {
            placeholders[i] = "?";
            parameters.add(values.get(i));
        }
        sql.append(String.join(", ", placeholders));
        sql.append(")");

        return new SqlAndParameters(sql.toString(), parameters);
    }
} 