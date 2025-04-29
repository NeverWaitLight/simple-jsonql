package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.CreateStatement;
import org.waitlight.simple.jsonql.statement.model.Field;
import org.waitlight.simple.jsonql.statement.model.JsonQLStatement;

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
    protected SqlAndParameters parseSql(JsonQLStatement statement) {
        if (!(statement instanceof CreateStatement)) {
            throw new IllegalArgumentException("Expected InsertStatement but got " + statement.getClass().getSimpleName());
        }
        CreateStatement createStatement = (CreateStatement) statement;
        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        sql.append("INSERT INTO ")
                .append(createStatement.getEntityId())
                .append(" (");

        // 处理字段
        List<String> fieldNames = new ArrayList<>();
        List<Object> fieldValues = new ArrayList<>();
        
        for (Field field : createStatement.getFields()) {
            fieldNames.add(field.getField());
            fieldValues.add(field.getValue());
            
            // TODO: Handle nested entities (future implementation)
            // This would require multiple SQL statements or stored procedures
            // For now, we'll log a warning if nested entities are found
            if (field.getValues() != null && !field.getValues().isEmpty()) {
                log.warn("Nested entities are found for field '{}' but are not supported in the current SQL implementation", 
                         field.getField());
            }
        }

        sql.append(String.join(", ", fieldNames));
        sql.append(") VALUES (");

        // 处理值
        String[] placeholders = new String[fieldValues.size()];
        for (int i = 0; i < fieldValues.size(); i++) {
            placeholders[i] = "?";
            parameters.add(fieldValues.get(i));
        }
        sql.append(String.join(", ", placeholders));
        sql.append(")");

        return new SqlAndParameters(sql.toString(), parameters);
    }
} 