package org.waitlight.simple.jsonql.jql;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Statement {
    SELECT,
    INSERT,
    UPDATE,
    DELETE;

    @JsonCreator
    public static Statement fromDisplayName(String displayName) {
        for (Statement statement : values()) {
            if (statement.name().equalsIgnoreCase(displayName)) {
                return statement;
            }
        }
        throw new IllegalArgumentException("No matching color found for: " + displayName);
    }
}