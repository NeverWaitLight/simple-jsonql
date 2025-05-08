package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.engine.sqlparser.ClauseSqlParser;
import org.waitlight.simple.jsonql.engine.sqlparser.PreparedSql;
import org.waitlight.simple.jsonql.engine.sqlparser.QuerySqlParser;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;
import org.waitlight.simple.jsonql.statement.QueryStatement;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class QueryEngine extends StatementEngine<QueryStatement> {
    private final ClauseSqlParser clauseSqlParser;
    private final QuerySqlParser querySqlParser;

    public QueryEngine(MetadataSources metadataSources) {
        super(metadataSources);
        this.clauseSqlParser = new ClauseSqlParser(metadataSources);
        this.querySqlParser = new QuerySqlParser(metadataSources);
    }

    @Override
    protected Object doExecute(Connection conn, PreparedSql<?> preparedSql) throws SQLException {
        if (preparedSql.getStatementType() != QueryStatement.class) {
            throw new IllegalArgumentException("QueryExecutor can only execute QueryStatements");
        }

        try (PreparedStatement stmt = conn.prepareStatement(preparedSql.getSql())) {
            List<Object> parameters = preparedSql.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return processResultSet(rs);
            }
        }
    }

    @Override
    protected PreparedSql<QueryStatement> parseSql(JsonQLStatement statement) {
        return querySqlParser.parseSql(statement);
    }

    private List<Map<String, Object>> processResultSet(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnName(i), rs.getObject(i));
            }
            results.add(row);
        }

        return results;
    }
} 