package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public abstract class StatementEngine<T extends JsonQLStatement> {
    protected final MetadataSources metadataSources;

    protected StatementEngine(MetadataSources metadataSources) {
        this.metadataSources = metadataSources;
    }

    public abstract Object execute(Connection conn, T stmt) throws SQLException;
} 