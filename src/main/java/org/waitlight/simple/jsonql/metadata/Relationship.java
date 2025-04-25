package org.waitlight.simple.jsonql.metadata;

public record Relationship(
        String alias,
        String foreignKey,
        String localKey,
        Type type
) {
    public enum Type {
        BELONGS_TO,
        HAS_ONE,
        HAS_MANY,
        MANY_TO_MANY
    }
}
