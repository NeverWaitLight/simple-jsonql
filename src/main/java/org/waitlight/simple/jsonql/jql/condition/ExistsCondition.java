package org.waitlight.simple.jsonql.jql.condition;

import org.waitlight.simple.jsonql.jql.JsonQL;

public class ExistsCondition extends Condition {
    private JsonQL subquery;
    private boolean not;

    public ExistsCondition() {
        setType("exists");
    }

    public JsonQL getSubquery() {
        return subquery;
    }

    public void setSubquery(JsonQL subquery) {
        this.subquery = subquery;
    }

    public boolean isNot() {
        return not;
    }

    public void setNot(boolean not) {
        this.not = not;
    }

    @Override
    public String toSql() {
        if (subquery == null) {
            return "";
        }
        return (not ? "NOT EXISTS (" : "EXISTS (") + subquery.toString() + ")";
    }
} 