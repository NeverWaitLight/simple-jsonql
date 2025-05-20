package org.waitlight.simple.jsonql.builder;

public class SqlBuildException extends Exception {
    public SqlBuildException(String message) {
        super(message);
    }

    public SqlBuildException(String message, Throwable cause) {
        super(message, cause);
    }
} 