package org.waitlight.simple.jsonql.jql.condition;

public class NullCondition extends Condition {
    private String field;
    private boolean not;

    public NullCondition() {
        setType("null");
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public boolean isNot() {
        return not;
    }

    public void setNot(boolean not) {
        this.not = not;
    }

    @Override
    public String toSql() {
        return field + (not ? " IS NOT NULL" : " IS NULL");
    }
} 