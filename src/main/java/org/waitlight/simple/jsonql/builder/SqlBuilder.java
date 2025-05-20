package org.waitlight.simple.jsonql.builder;

import org.waitlight.simple.jsonql.statement.JsonQLStatement;

public interface SqlBuilder<T extends JsonQLStatement> {
    PreparedSql<T> build(T stmt) throws SqlBuildeException;
}
