package org.waitlight.simple.jsonql.execute;

public class JsonQLExecuteException extends Exception {
    public JsonQLExecuteException(String message) {
        super(message);
    }

    public JsonQLExecuteException(String message, Throwable cause) {
        super(message, cause);
    }
} 