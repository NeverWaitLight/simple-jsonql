package org.waitlight.simple.jsonql.statement.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Operator {
   ;

    private final String symbol;

    Operator(String symbol) {
        this.symbol = symbol;
    }

    @JsonValue
    public String getValue() {
        return name().toLowerCase();
    }

    public String getSymbol() {
        return symbol;
    }

    @JsonCreator
    public static Operator fromValue(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown operator: " + value);
        }
    }
}
