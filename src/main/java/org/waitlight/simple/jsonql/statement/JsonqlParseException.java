package org.waitlight.simple.jsonql.statement;

public class JsonqlParseException extends Exception {
    public JsonqlParseException(String message) {
        super(message);
    }

    public JsonqlParseException(String message, Throwable cause) {
        super(message, cause);
    }
} 