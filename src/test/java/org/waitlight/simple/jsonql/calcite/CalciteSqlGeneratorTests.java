package org.waitlight.simple.jsonql.calcite;

import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 测试使用Apache Calcite生成SQL语句
 */
public class CalciteSqlGeneratorTests {

    private static StatementParser statementParser;

    @BeforeAll
    public static void setUp() {
        statementParser = new StatementParser();
    }

    @Test
    public void testBasicJsonToSql() throws Exception {
        // 1. 基本JSON查询转SQL
        String jsonQuery = """
                {
                    "statement": "select",
                    "appId": "123456",
                    "formId": "89757",
                    "entityId": "t_user",
                    "page": {"size": 10, "number": 1}
                }
                """;

        SelectStatement selectStatement = (SelectStatement) statementParser.parse2Stmt(jsonQuery);

        // 创建Schema和表定义
        SchemaPlus schema = CalciteSchema.createRootSchema(false).plus();

        // 定义用户表结构
        RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
        RelDataTypeFactory.Builder userTypeBuilder = typeFactory.builder();
        userTypeBuilder.add("id", SqlTypeName.INTEGER);
        userTypeBuilder.add("name", SqlTypeName.VARCHAR);
        userTypeBuilder.add("age", SqlTypeName.INTEGER);
        userTypeBuilder.add("sex", SqlTypeName.INTEGER);
        RelDataType userRowType = userTypeBuilder.build();

        // 添加用户表到schema
        Table userTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return userRowType;
            }
        };
        schema.add("t_user", userTable);

        // 创建RelBuilder
        FrameworkConfig config = Frameworks.newConfigBuilder()
                .defaultSchema(schema)
                .build();

        RelBuilder builder = RelBuilder.create(config);

        // 构建查询
        builder.scan("t_user");
        builder.project(builder.fields());

        // 生成SQL
        RelNode relNode = builder.build();
        SqlDialect dialect = SqlDialect.DatabaseProduct.MYSQL.getDialect();
        RelToSqlConverter converter = new RelToSqlConverter(dialect);
        SqlNode sqlNode = converter.visitRoot(relNode).asStatement();
        String sql = sqlNode.toSqlString(dialect).getSql();

        System.out.println("基本查询SQL: " + sql);

        // 验证生成的SQL
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM `t_user`"));
    }

    @Test
    public void testFilterConditions() throws Exception {
        // 1. 创建Schema和表定义
        SchemaPlus schema = CalciteSchema.createRootSchema(false).plus();

        // 定义用户表结构
        RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
        RelDataTypeFactory.Builder userTypeBuilder = typeFactory.builder();
        userTypeBuilder.add("id", SqlTypeName.INTEGER);
        userTypeBuilder.add("name", SqlTypeName.VARCHAR);
        userTypeBuilder.add("age", SqlTypeName.INTEGER);
        userTypeBuilder.add("sex", SqlTypeName.INTEGER); // 1=男, 0=女
        RelDataType userRowType = userTypeBuilder.build();

        // 添加用户表到schema
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

        // 3. 构建查询: SELECT * FROM t_user WHERE name LIKE 'tom%' AND (age > 40 OR sex =
        // 1)
        builder.scan("t_user");

        // 构建 name LIKE 'tom%'
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

        // 构建 (age > 40 OR sex = 1)
        RexNode ageOrSexCondition = builder.or(ageCondition, sexCondition);

        // 构建 name LIKE 'tom%' AND (age > 40 OR sex = 1)
        builder.filter(builder.and(nameCondition, ageOrSexCondition));

        // 所有字段
        builder.project(builder.fields());

        // 4. 生成SQL
        RelNode relNode = builder.build();
        SqlDialect dialect = SqlDialect.DatabaseProduct.MYSQL.getDialect();
        RelToSqlConverter converter = new RelToSqlConverter(dialect);
        SqlNode sqlNode = converter.visitRoot(relNode).asStatement();
        String sql = sqlNode.toSqlString(dialect).getSql();

        System.out.println("带条件的查询SQL: " + sql);

        // 5. 验证生成的SQL
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("`name` LIKE 'tom%'"));
        assertTrue(sql.contains("`age` > 40"));
        assertTrue(sql.contains("`sex` = 1"));
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("OR"));
    }

    @Test
    public void testTableJoin() {
        // 1. 创建Schema
        SchemaPlus schema = CalciteSchema.createRootSchema(false).plus();
        RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();

        // 2. 定义用户表结构
        RelDataTypeFactory.Builder userTypeBuilder = typeFactory.builder();
        userTypeBuilder.add("id", SqlTypeName.INTEGER);
        userTypeBuilder.add("name", SqlTypeName.VARCHAR);
        userTypeBuilder.add("age", SqlTypeName.INTEGER);
        RelDataType userRowType = userTypeBuilder.build();

        // 添加用户表到schema
        Table userTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return userRowType;
            }
        };
        schema.add("t_user", userTable);

        // 3. 定义订单表结构
        RelDataTypeFactory.Builder orderTypeBuilder = typeFactory.builder();
        orderTypeBuilder.add("id", SqlTypeName.INTEGER);
        orderTypeBuilder.add("user_id", SqlTypeName.INTEGER); // 关联用户表的外键
        orderTypeBuilder.add("order_name", SqlTypeName.VARCHAR);
        orderTypeBuilder.add("amount", SqlTypeName.DECIMAL);
        RelDataType orderRowType = orderTypeBuilder.build();

        // 添加订单表到schema
        Table orderTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return orderRowType;
            }
        };
        schema.add("t_order", orderTable);

        // 4. 创建RelBuilder
        FrameworkConfig config = Frameworks.newConfigBuilder()
                .defaultSchema(schema)
                .build();
        RelBuilder builder = RelBuilder.create(config);

        // 5. 构建查询:
        // SELECT u.id, u.name, o.order_name, o.amount
        // FROM t_user u LEFT JOIN t_order o ON u.id = o.user_id
        // WHERE u.age > 30

        // 扫描user表
        builder.scan("t_user");

        // 扫描order表
        builder.scan("t_order");

        // 执行LEFT JOIN
        builder.join(
                JoinRelType.LEFT,
                builder.equals(
                        builder.field(2, 0, "id"),
                        builder.field(2, 1, "user_id")));

        // 添加WHERE条件: u.age > 30
        builder.filter(
                builder.call(
                        SqlStdOperatorTable.GREATER_THAN,
                        builder.field(2, 0, "age"),
                        builder.literal(30)));

        // 选择字段
        builder.project(
                builder.field(2, 0, "id"),
                builder.field(2, 0, "name"),
                builder.field(2, 1, "order_name"),
                builder.field(2, 1, "amount"));

        // 7. 生成SQL
        RelNode relNode = builder.build();
        SqlDialect dialect = SqlDialect.DatabaseProduct.MYSQL.getDialect();
        RelToSqlConverter converter = new RelToSqlConverter(dialect);
        SqlNode sqlNode = converter.visitRoot(relNode).asStatement();
        String sql = sqlNode.toSqlString(dialect).getSql();

        System.out.println("表连接的查询SQL: " + sql);

        // 8. 验证生成的SQL
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM `t_user`"));
        assertTrue(sql.contains("LEFT JOIN `t_order`"));
        assertTrue(sql.contains("ON `t_user`.`id` = `t_order`.`user_id`"));
        assertTrue(sql.contains("WHERE `t_user`.`age` > 30"));
    }

    /**
     * 打印FrameworkConfig中的表定义
     */
    private void printTableDefinitions(FrameworkConfig config) {
        RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();
        SchemaPlus schema = config.getDefaultSchema();

        System.out.println("\n=== 当前Schema中的表定义 ===");

        // 获取所有表名
        for (String tableName : schema.getTableNames()) {
            System.out.println("\n表名: " + tableName);

            // 获取表对象
            Table table = schema.getTable(tableName);

            // 获取表的行类型定义
            RelDataType rowType = table.getRowType(typeFactory);

            // 打印列信息
            System.out.println("  列定义:");
            List<RelDataTypeField> fields = rowType.getFieldList();
            for (RelDataTypeField field : fields) {
                String fieldName = field.getName();
                SqlTypeName typeName = field.getType().getSqlTypeName();
                boolean nullable = field.getType().isNullable();

                System.out.println("    - " + fieldName +
                        ": " + typeName +
                        (nullable ? " (可为空)" : " (非空)"));
            }
        }
        System.out.println("\n=== 表定义输出结束 ===\n");
    }

    /**
     * 生成CREATE TABLE语句
     */
    private String generateCreateTableSql(String tableName, RelDataType rowType) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE `").append(tableName).append("` (\n");

        List<RelDataTypeField> fields = rowType.getFieldList();
        ArrayList<String> columnDefs = new ArrayList<>();

        for (RelDataTypeField field : fields) {
            String fieldName = field.getName();
            SqlTypeName typeName = field.getType().getSqlTypeName();
            boolean nullable = field.getType().isNullable();

            StringBuilder columnDef = new StringBuilder();
            columnDef.append("  `").append(fieldName).append("` ");

            // 添加类型定义
            switch (typeName) {
                case INTEGER:
                    columnDef.append("INT");
                    break;
                case VARCHAR:
                    int precision = field.getType().getPrecision();
                    columnDef.append("VARCHAR(").append(precision > 0 ? precision : 255).append(")");
                    break;
                case CHAR:
                    columnDef.append("CHAR(").append(field.getType().getPrecision()).append(")");
                    break;
                case DECIMAL:
                    int totalDigits = field.getType().getPrecision();
                    int fractionDigits = field.getType().getScale();
                    columnDef.append("DECIMAL(").append(totalDigits).append(", ").append(fractionDigits).append(")");
                    break;
                case DOUBLE:
                    columnDef.append("DOUBLE");
                    break;
                case FLOAT:
                    columnDef.append("FLOAT");
                    break;
                case BOOLEAN:
                    columnDef.append("BOOLEAN");
                    break;
                case DATE:
                    columnDef.append("DATE");
                    break;
                case TIME:
                    columnDef.append("TIME");
                    break;
                case TIMESTAMP:
                    columnDef.append("TIMESTAMP");
                    break;
                case TINYINT:
                    columnDef.append("TINYINT");
                    break;
                case SMALLINT:
                    columnDef.append("SMALLINT");
                    break;
                case BIGINT:
                    columnDef.append("BIGINT");
                    break;
                default:
                    columnDef.append(typeName.toString());
            }

            // 添加NULL/NOT NULL约束
            if (!nullable) {
                columnDef.append(" NOT NULL");
            }

            // 为ID字段添加主键约束
            if (fieldName.equals("id")) {
                columnDef.append(" PRIMARY KEY");
            }

            columnDefs.add(columnDef.toString());
        }

        sb.append(String.join(",\n", columnDefs));
        sb.append("\n)");

        return sb.toString();
    }
}