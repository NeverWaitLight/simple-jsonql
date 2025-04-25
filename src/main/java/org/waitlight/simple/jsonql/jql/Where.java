package org.waitlight.simple.jsonql.jql;

import org.waitlight.simple.jsonql.jql.condition.Condition;

public record Where(
        Condition condition
) {
    public String toSql() {
        return condition != null ? condition.toSql() : "";
    }
}