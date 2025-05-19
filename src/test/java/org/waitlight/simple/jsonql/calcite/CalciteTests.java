package org.waitlight.simple.jsonql.calcite;

import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.RelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.statement.SelectStatement;
import org.waitlight.simple.jsonql.statement.StatementParser;
import org.waitlight.simple.jsonql.statement.model.Condition;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CalciteTests {

    private static StatementParser statementParser;

    @BeforeAll
    public static void setUp() {
        statementParser = new StatementParser();
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

    @Test
    public void testDirectCalciteApiWithMultipleConditions() {
        // 1. 创建Schema和表定义
        SchemaPlus schema = CalciteSchema.createRootSchema(false).plus();

        // 定义产品表结构
        RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
        RelDataTypeFactory.Builder productTypeBuilder = typeFactory.builder();
        productTypeBuilder.add("id", SqlTypeName.INTEGER);
        productTypeBuilder.add("name", SqlTypeName.VARCHAR);
        productTypeBuilder.add("price", SqlTypeName.DECIMAL);
        productTypeBuilder.add("category", SqlTypeName.VARCHAR);
        productTypeBuilder.add("stock", SqlTypeName.INTEGER);
        RelDataType productRowType = productTypeBuilder.build();

        // 添加产品表到schema
        Table productTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return productRowType;
            }
        };
        schema.add("product", productTable);

        // 2. 创建RelBuilder
        FrameworkConfig config = Frameworks.newConfigBuilder()
                .defaultSchema(schema)
                .build();
        RelBuilder builder = RelBuilder.create(config);

        // 3. 构建查询
        // SELECT id, name, price, category FROM product
        // WHERE (price > 100.0 AND category = 'electronics')
        // OR (stock < 10 AND category = 'books')
        builder.scan("product");

        // 构建复杂条件: (price > 100.0 AND category = 'electronics')
        RexNode priceCondition = builder.call(
                SqlStdOperatorTable.GREATER_THAN,
                builder.field("price"),
                builder.literal(100.0));

        RexNode categoryElectronicsCondition = builder.equals(
                builder.field("category"),
                builder.literal("electronics"));

        RexNode firstCondition = builder.and(priceCondition, categoryElectronicsCondition);

        // 构建复杂条件: (stock < 10 AND category = 'books')
        RexNode stockCondition = builder.call(
                SqlStdOperatorTable.LESS_THAN,
                builder.field("stock"),
                builder.literal(10));

        RexNode categoryBooksCondition = builder.equals(
                builder.field("category"),
                builder.literal("books"));

        RexNode secondCondition = builder.and(stockCondition, categoryBooksCondition);

        // 组合两个条件: condition1 OR condition2
        builder.filter(
                builder.or(firstCondition, secondCondition));

        // 选择需要的字段
        builder.project(
                builder.field("id"),
                builder.field("name"),
                builder.field("price"),
                builder.field("category"));

        // 4. 生成SQL
        RelNode relNode = builder.build();
        SqlDialect dialect = SqlDialect.DatabaseProduct.MYSQL.getDialect();
        RelToSqlConverter converter = new RelToSqlConverter(dialect);
        SqlNode sqlNode = converter.visitRoot(relNode).asStatement();
        String sql = sqlNode.toSqlString(dialect).getSql();

        System.out.println("直接使用Calcite API生成的多条件SQL: " + sql);

        // 5. 验证生成的SQL
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("`price` > 100"));
        assertTrue(sql.contains("`category` = 'electronics'"));
        assertTrue(sql.contains("`stock` < 10"));
        assertTrue(sql.contains("`category` = 'books'"));
        assertTrue(sql.contains("OR"));
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testNameLikeAndAgeOrSex() {
        // 1. 创建Schema和用户表定义
        SchemaPlus schema = CalciteSchema.createRootSchema(false).plus();

        // 定义用户表结构 - 使用"user_info"代替"user"避免关键字问题
        RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
        RelDataTypeFactory.Builder userTypeBuilder = typeFactory.builder();
        userTypeBuilder.add("id", SqlTypeName.INTEGER);
        userTypeBuilder.add("name", SqlTypeName.VARCHAR);
        userTypeBuilder.add("age", SqlTypeName.INTEGER);
        userTypeBuilder.add("sex", SqlTypeName.INTEGER); // 1=男, 0=女
        RelDataType userRowType = userTypeBuilder.build();

        // 添加用户表到schema (使用user_info代替user)
        Table userTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return userRowType;
            }
        };
        schema.add("t_user", userTable);

        // 2. 创建RelBuilder
        FrameworkConfig config = Frameworks.newConfigBuilder()
                .defaultSchema(schema)
                .build();
        RelBuilder builder = RelBuilder.create(config);

        // 3. 构建：select * from user_info where name like 'tom%' and (age > 40 or sex =
        // 1)
        builder.scan("t_user");

        // 构建 name like 'tom%'
        RexNode nameCondition = builder.call(
                SqlStdOperatorTable.LIKE,
                builder.field("name"),
                builder.literal("tom%"));

        // 构建 age > 40
        RexNode ageCondition = builder.call(
                SqlStdOperatorTable.GREATER_THAN,
                builder.field("age"),
                builder.literal(40));

        // 构建 sex = 1
        RexNode sexCondition = builder.equals(
                builder.field("sex"),
                builder.literal(1));

        // 构建 (age > 40 or sex = 1)
        RexNode ageOrSexCondition = builder.or(ageCondition, sexCondition);

        // 构建最终条件: name like 'tom%' and (age > 40 or sex = 1)
        RexNode finalCondition = builder.and(nameCondition, ageOrSexCondition);

        // 应用过滤条件
        builder.filter(finalCondition);

        // 4. 生成SQL
        RelNode relNode = builder.build();
        SqlDialect dialect = SqlDialect.DatabaseProduct.MYSQL.getDialect();
        RelToSqlConverter converter = new RelToSqlConverter(dialect);
        SqlNode sqlNode = converter.visitRoot(relNode).asStatement();
        String sql = sqlNode.toSqlString(dialect).getSql();

        // 去掉换行符，保持SQL在一行内显示
        sql = sql.replaceAll("[\r\n]+", " ");

        System.out.println("生成的SQL: " + sql);

        // 5. 验证生成的SQL
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM `t_user`")); // 验证新表名
        assertTrue(sql.contains("`name` LIKE 'tom%'"));
        assertTrue(sql.contains("`age` > 40"));
        assertTrue(sql.contains("`sex` = 1"));
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("OR"));

        // 验证括号存在于 OR 条件周围
        boolean hasProperParentheses = sql.matches(".*`name` LIKE 'tom%' AND \\(.*OR.*\\).*");
        assertTrue(hasProperParentheses, "SQL中应该有正确的括号包围OR条件: " + sql);

        // 直接通过手动拼接构建正确的SQL (作为参考)
        String manualSql = "SELECT * FROM `user` WHERE `name` LIKE 'tom%' AND (`age` > 40 OR `sex` = 1)";
        System.out.println("手动构建的SQL参考: " + manualSql);
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