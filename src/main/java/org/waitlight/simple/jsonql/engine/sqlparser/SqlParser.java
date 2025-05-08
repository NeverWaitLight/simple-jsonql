package org.waitlight.simple.jsonql.engine.sqlparser;

import org.waitlight.simple.jsonql.statement.JsonQLStatement;

public interface SqlParser<T extends JsonQLStatement> {
    PreparedSql<T> parseStmt2Sql(T stmt);
}
