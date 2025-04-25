package org.waitlight.simple.jsonql.jql.condition;

import java.util.List;

public class BetweenCondition extends Condition {
    private String field;
    private List<Object> values;

    public BetweenCondition() {
        setType("between");
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

    @Override
    public String toSql() {
        if (values == null || values.size() != 2) {
            return "";
        }
        String start = values.get(0) instanceof String ? 
            "'" + values.get(0) + "'" : String.valueOf(values.get(0));
        String end = values.get(1) instanceof String ? 
            "'" + values.get(1) + "'" : String.valueOf(values.get(1));
        return field + " BETWEEN " + start + " AND " + end;
    }
} 