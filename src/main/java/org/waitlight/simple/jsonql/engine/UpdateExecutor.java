package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.UpdateStatement;
import org.waitlight.simple.jsonql.statement.model.Field;
import org.waitlight.simple.jsonql.statement.model.JsonQLStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UpdateExecutor extends StatementExecutor<UpdateStatement> {
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
    protected Object doExecute(Connection conn, PreparedSql<?> preparedSql) throws SQLException {
        if (preparedSql.getStatementType() != UpdateStatement.class) {
            throw new IllegalArgumentException("UpdateExecutor can only execute UpdateStatements");
        }

        try (PreparedStatement stmt = conn.prepareStatement(preparedSql.getSql())) {
            List<Object> parameters = preparedSql.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            return stmt.executeUpdate();
        }
    }

    @Override
    protected List<PreparedSql<UpdateStatement>> parseSql(JsonQLStatement statement) {
        if (!(statement instanceof UpdateStatement updateStatement)) {
            throw new IllegalArgumentException("Expected UpdateStatement but got " + statement.getClass().getSimpleName());
        }

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ")
                .append(updateStatement.getEntityId())
                .append(" SET ");

        List<String> setClauses = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        
        for (Field field : updateStatement.getFields()) {
            setClauses.add(field.getField() + " = ?");
            parameters.add(field.getValue());
        }
        
        sql.append(String.join(", ", setClauses));

        // 添加 WHERE 子句
        sql.append(" WHERE id = ?");
        parameters.add(updateStatement.getDataId());

        List<PreparedSql<UpdateStatement>> result = new ArrayList<>();
        PreparedSql<UpdateStatement> sqlObj = new PreparedSql<>();
        sqlObj.setSql(sql.toString());
        sqlObj.setParameters(parameters);
        sqlObj.setStatementType(UpdateStatement.class);
        result.add(sqlObj);
        return result;
    }
}
