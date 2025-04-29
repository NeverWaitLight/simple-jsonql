package org.waitlight.simple.jsonql.statement.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MethodType {
    EQ("="),
    NE("<>"),
    GT(">"),
    LT("<"),
    GE(">="),
    LE("<="),
    LIKE("LIKE"),
    IN("IN"),
    BETWEEN("BETWEEN"),
    IS("IS"),
    EXISTS("EXISTS"),

    AND("AND"),
    OR("OR"),
    NOT("NOT");

    private final String symbol;

    MethodType(String symbol) {
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
    public static MethodType fromValue(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown operator type: " + value);
        }
    }
}
