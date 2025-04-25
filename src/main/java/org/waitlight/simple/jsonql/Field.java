package org.waitlight.simple.jsonql;

public record Field(
        String column,
        String type,
        boolean primary
) {
}
