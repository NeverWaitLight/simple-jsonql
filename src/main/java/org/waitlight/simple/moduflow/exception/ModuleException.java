package org.waitlight.simple.moduflow.exception;

/**
 * 模块执行异常
 */
public class ModuleException extends RuntimeException {
    public ModuleException(String message) {
        super(message);
    }

    public ModuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
