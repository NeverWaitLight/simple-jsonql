package org.waitlight.simple.jsonql.metadata;

public class MetadataException extends RuntimeException {
    public MetadataException(String message) {
        super(message);
    }

    public MetadataException(String message, Object... parameters) {
        super(message.formatted(parameters));
    }
}