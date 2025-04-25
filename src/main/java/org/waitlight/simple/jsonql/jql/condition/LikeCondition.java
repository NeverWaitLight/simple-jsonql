package org.waitlight.simple.jsonql.jql.condition;

public class LikeCondition extends Condition {
    private String field;
    private String pattern;
    private boolean not;

    public LikeCondition() {
        setType("like");
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean isNot() {
        return not;
    }

    public void setNot(boolean not) {
        this.not = not;
    }

    @Override
    public String toSql() {
        return field + (not ? " NOT LIKE '" : " LIKE '") + pattern + "'";
    }
} 