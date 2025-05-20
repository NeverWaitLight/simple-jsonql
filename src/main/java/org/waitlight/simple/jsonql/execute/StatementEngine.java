package org.waitlight.simple.jsonql.execute;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.builder.SqlBuildException;
import org.waitlight.simple.jsonql.execute.result.ExecuteResult;
import org.waitlight.simple.jsonql.metadata.Metadata;
import org.waitlight.simple.jsonql.metadata.MetadataSource;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public abstract class StatementEngine<T extends JsonQLStatement, R extends ExecuteResult> {
    protected final MetadataSource metadataSource;
    protected Metadata metadata;

    protected StatementEngine(MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
    }

    public abstract R execute(Connection conn, T stmt) throws SQLException, SqlBuildException;
} 