package org.waitlight.simple.jsonql.calcite;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.RelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.statement.SelectStatement;
import org.waitlight.simple.jsonql.statement.StatementParser;
import org.waitlight.simple.jsonql.statement.model.Condition;

public class CalciteSqlGeneratorTests {

    private static StatementParser statementParser;

    @BeforeAll
    public static void setUp() {
        statementParser = new StatementParser();
    }

    @Test
    public void testBasicSelectQuery() throws Exception {
        // 基本查询JSON，参照SelectEngineTest中的testBasicQuery
        String query = """
                {
                    "statement": "select",
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "page": {"size": 10, "number": 1}
                }
                """;

        // 解析JSON为SelectStatement
        SelectStatement selectStatement = (SelectStatement) statementParser.parse2Stmt(query);

        // 生成SQL
        String sql = generateSql(selectStatement);
        System.out.println("生成的SQL: " + sql);

        // 验证生成的SQL
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM `user`"));
    }

    @Test
    public void testQueryWithFilters() throws Exception {
        // 带过滤条件的查询，参照SelectEngineTest中的testQueryWithFilters
        String query = """
                {
                    "statement": "select",
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "user",
                    "filters": {
                        "rel": "and",
                        "conditions": [
                            {"field": "id", "method": "eq", "value": 1}
                        ]
                    },
                    "page": {"size": 10, "number": 1}
                }
                """;

        // 解析JSON为SelectStatement
        SelectStatement selectStatement = (SelectStatement) statementParser.parse2Stmt(query);

        // 生成SQL
        String sql = generateSql(selectStatement);
        System.out.println("生成的带过滤条件的SQL: " + sql);

        // 验证生成的SQL
        assertNotNull(sql);
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("`id` = 1"));
    }

    /**
     * 根据SelectStatement生成SQL
     */
    public static String generateSql(SelectStatement selectStatement) {
        // 1. 创建一个内存中的Schema（模拟数据库表）
        SchemaPlus schema = CalciteSchema.createRootSchema(false).plus();

        // 创建表定义
        final RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
        final RelDataTypeFactory.Builder typeBuilder = typeFactory.builder();
        typeBuilder.add("id", SqlTypeName.BIGINT);
        typeBuilder.add("name", SqlTypeName.VARCHAR);
        typeBuilder.add("age", SqlTypeName.INTEGER);
        final RelDataType userRowType = typeBuilder.build();

        // 添加表到schema
        Table userTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return userRowType;
            }
        };
        schema.add("user", userTable);

        // 3. 创建Frameworks配置和RelBuilder
        FrameworkConfig config = Frameworks.newConfigBuilder()
                .defaultSchema(schema)
                .build();
        RelBuilder relBuilder = RelBuilder.create(config);

        // 4. 构建FROM子句
        relBuilder.scan(selectStatement.getEntityId());

        // 5. 构建WHERE子句（如果有过滤条件）
        if (selectStatement.getFilters() != null && selectStatement.getFilters().getConditions() != null) {
            var conditions = selectStatement.getFilters().getConditions();

            for (Condition condition : conditions) {
                String field = condition.getField();
                String methodStr = condition.getMethod().toString();
                Object value = condition.getValue();

                if ("eq".equals(methodStr)) {
                    relBuilder.filter(
                            relBuilder.equals(
                                    relBuilder.field(field),
                                    relBuilder.literal(value)));
                }
                // 可以根据需要添加其他条件处理（gt, lt, like等）
            }
        }

        // 6. 构建SELECT子句（默认选择所有字段）
        relBuilder.project(relBuilder.fields());

        // 7. 生成SQL
        RelNode relNode = relBuilder.build();
        SqlDialect dialect = SqlDialect.DatabaseProduct.HSQLDB.getDialect();
        RelToSqlConverter converter = new RelToSqlConverter(dialect);

        // 使用visitRoot方法替代visitChild
        SqlNode sqlNode = converter.visitRoot(relNode).asStatement();

        return sqlNode.toSqlString(dialect).getSql();
    }
}