package org.waitlight.simple.jsonql.engine;

import java.util.ArrayList;
import java.util.List;

import org.waitlight.simple.jsonql.statement.CreateStatement;
import org.waitlight.simple.jsonql.statement.model.JsonQLStatement;

public record PreparedSql<T extends JsonQLStatement>(
        String sql,
        List<Object> parameters,
        List<CreateStatement> nestedCreateStatements,
        Class<T> statementType
) {
    public PreparedSql {
        if (nestedCreateStatements == null) {
            nestedCreateStatements = new ArrayList<>();
        }
    }

    public PreparedSql(String sql, List<Object> parameters, Class<T> statementType) {
        this(sql, parameters, new ArrayList<>(), statementType);
    }

    public int getNestedEntityCount() {
        return nestedCreateStatements.size();
    }
}