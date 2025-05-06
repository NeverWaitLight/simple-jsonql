package org.waitlight.simple.jsonql.engine;

import java.util.List;

import org.waitlight.simple.jsonql.statement.model.JsonQLStatement;

public record PreparedSql<T extends JsonQLStatement>(
        String sql,
        List<Object> parameters,
        Class<T> statementType
) {
    public PreparedSql {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters list cannot be null");
        }
    }
}