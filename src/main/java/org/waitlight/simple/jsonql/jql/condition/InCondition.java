package org.waitlight.simple.jsonql.jql.condition;

import java.util.List;

public class InCondition extends Condition {
    private String field;
    private List<Object> values;
    private boolean not;

    public InCondition() {
        setType("in");
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }

    public boolean isNot() {
        return not;
    }

    public void setNot(boolean not) {
        this.not = not;
    }

    @Override
    public String toSql() {
        if (values == null || values.isEmpty()) {
            return "";
        }
        String valuesStr = values.stream()
            .map(v -> v instanceof String ? "'" + v + "'" : String.valueOf(v))
            .collect(java.util.stream.Collectors.joining(", "));
        return field + (not ? " NOT IN (" : " IN (") + valuesStr + ")";
    }
} 