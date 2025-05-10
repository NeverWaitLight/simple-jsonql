package org.waitlight.simple.jsonql.engine;

import java.sql.Connection;
import java.sql.SQLException;

import org.waitlight.simple.jsonql.engine.result.ExecuteResult;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class StatementEngine<T extends JsonQLStatement, R extends ExecuteResult> {
    protected final MetadataSources metadataSources;

    protected StatementEngine(MetadataSources metadataSources) {
        this.metadataSources = metadataSources;
    }

    public abstract R execute(Connection conn, T stmt) throws SQLException;
} 