package org.waitlight.simple.jsonql.engine;

import java.util.ArrayList;
import java.util.List;

import org.waitlight.simple.jsonql.statement.model.JsonQLStatement;

import lombok.Data;

@Data
public class PreparedSql<T extends JsonQLStatement> {
    private String sql;
    private List<Object> parameters;
    private Class<T> statementType;
    private List<PreparedSql<T>> nestedSqls = new ArrayList<>();

    public void addNestedSqls(PreparedSql<T> nestedSql) {
        this.nestedSqls.add(nestedSql);
    }
}