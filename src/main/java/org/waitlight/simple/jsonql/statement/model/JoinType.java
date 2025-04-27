package org.waitlight.simple.jsonql.statement.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum JoinType {
    INNER,
    LEFT,
    RIGHT;

    @JsonValue
    public String getValue() {
        return name();
    }

    @JsonCreator
    public static JoinType fromValue(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown join type: " + value);
        }
    }
}
