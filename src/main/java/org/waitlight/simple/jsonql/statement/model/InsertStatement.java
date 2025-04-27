package org.waitlight.simple.jsonql.statement.model;

import java.util.Map;

public class InsertStatement extends JsonqlStatement {
    private String into;
    private Map<String, Object> values;

    public String getInto() {
        return into;
    }

    public void setInto(String into) {
        this.into = into;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }
} 