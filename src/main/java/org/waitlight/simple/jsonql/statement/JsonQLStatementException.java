package org.waitlight.simple.jsonql.statement;

public class JsonQLStatementException extends Exception {
    public JsonQLStatementException(String message) {
        super(message);
    }

    public JsonQLStatementException(String message, Throwable cause) {
        super(message, cause);
    }
} 