package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.engine.sqlparser.ClauseSqlParser;
import org.waitlight.simple.jsonql.engine.sqlparser.PreparedSql;
import org.waitlight.simple.jsonql.engine.sqlparser.SelectSqlParser;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.SelectStatement;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SelectEngine extends StatementEngine<SelectStatement> {
    private final ClauseSqlParser clauseSqlParser;
    private final SelectSqlParser selectSqlParser;

    public SelectEngine(MetadataSources metadataSources) {
        super(metadataSources);
        this.clauseSqlParser = new ClauseSqlParser(metadataSources);
        this.selectSqlParser = new SelectSqlParser(metadataSources);
    }

    @Override
    public Object execute(Connection conn, SelectStatement statement) throws SQLException {
        PreparedSql<SelectStatement> preparedSql = selectSqlParser.parseSql(statement);

        if (preparedSql.getStatementType() != SelectStatement.class) {
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