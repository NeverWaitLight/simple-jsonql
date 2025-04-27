package org.waitlight.simple.jsonql.statement.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Direction {
    ASC,
    DESC;

    @JsonValue
    public String getValue() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static Direction fromValue(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown direction: " + value);
        }
    }
}
