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
import org.waitlight.simple.jsonql.statement.model.FilterCondition;

import java.util.List;
import java.util.ArrayList;

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
        SelectStatement selectStatement = (SelectStatement) statementParser.parse(query);

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

    @Test
    public void testStudentClassLeftJoin() {
        // 1. 创建Schema
        SchemaPlus schema = CalciteSchema.createRootSchema(false).plus();
        RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();

        // 2. 定义学生表结构
        RelDataTypeFactory.Builder studentTypeBuilder = typeFactory.builder();
        studentTypeBuilder.add("id", SqlTypeName.INTEGER);
        studentTypeBuilder.add("clazz_id", SqlTypeName.BIGINT); // 关联班级表的外键
        studentTypeBuilder.add("name", typeFactory.createSqlType(SqlTypeName.VARCHAR, 20));
        studentTypeBuilder.add("price", typeFactory.createSqlType(SqlTypeName.DOUBLE, 20, 2));
        studentTypeBuilder.add("age", SqlTypeName.INTEGER);
        studentTypeBuilder.add("deleted", SqlTypeName.BOOLEAN);
        RelDataType studentRowType = studentTypeBuilder.build();

        // 添加学生表到schema
        Table studentTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return studentRowType;
            }
        };
        schema.add("t_student", studentTable);

        // 3. 定义班级表结构
        RelDataTypeFactory.Builder clazzTypeBuilder = typeFactory.builder();
        clazzTypeBuilder.add("id", SqlTypeName.INTEGER);
        clazzTypeBuilder.add("name", SqlTypeName.VARCHAR);
        clazzTypeBuilder.add("grade", SqlTypeName.INTEGER);
        RelDataType clazzRowType = clazzTypeBuilder.build();

        // 添加班级表到schema
        Table clazzTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return clazzRowType;
            }
        };
        schema.add("t_clazz", clazzTable);

        // 4. 创建RelBuilder
        FrameworkConfig config = Frameworks.newConfigBuilder()
                .defaultSchema(schema)
                .build();

        // 输出表定义
        printTableDefinitions(config);

        RelBuilder builder = RelBuilder.create(config);

        // 5. 构建左连接查询：
        // SELECT stu.name, clazz.name as clazz_name
        // FROM t_student stu
        // LEFT JOIN t_clazz clazz ON stu.clazz_id = clazz.id

        // 扫描学生表，并设置别名为stu
        builder.scan("t_student").as("stu");

        // 扫描班级表，并设置别名为clazz
        builder.scan("t_clazz").as("clazz");

        // 构建连接条件：stu.clazz_id = clazz.id
        RexNode joinCondition = builder.equals(
                builder.field(2, "stu", "clazz_id"), // 左表(stu)的clazz_id字段
                builder.field(2, "clazz", "id")); // 右表(clazz)的id字段

        // 执行左连接
        builder.join(JoinRelType.LEFT, joinCondition);

        // 选择需要的字段：stu.name, clazz.name (作为clazz_name)
        builder.project(
                builder.field("stu", "name"),
                builder.alias(builder.field("clazz", "name"), "clazz_name"));

        // 6. 生成SQL
        RelNode relNode = builder.build();
        SqlDialect dialect = SqlDialect.DatabaseProduct.MYSQL.getDialect();
        RelToSqlConverter converter = new RelToSqlConverter(dialect);
        SqlNode sqlNode = converter.visitRoot(relNode).asStatement();
        String sql = sqlNode.toSqlString(dialect).getSql();

        // 去掉换行符，保持SQL在一行内显示
        sql = sql.replaceAll("[\r\n]+", " ");

        System.out.println("学生-班级左连接SQL: " + sql);

        // 7. 验证生成的SQL
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("`stu`.`name`"));
        assertTrue(sql.contains("`clazz`.`name` AS `clazz_name`"));
        assertTrue(sql.contains("FROM `t_student` AS `stu`"));
        assertTrue(sql.contains("LEFT JOIN `t_clazz` AS `clazz`"));
        assertTrue(sql.contains("ON `stu`.`clazz_id` = `clazz`.`id`"));
    }

    @Test
    public void testAllDataTypesTableAndQuery() {
        // 1. 创建Schema
        SchemaPlus schema = CalciteSchema.createRootSchema(false).plus();
        RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl();

        // 2. 定义包含各种数据类型的表结构
        RelDataTypeFactory.Builder allTypesBuilder = typeFactory.builder();
        // 整数类型
        allTypesBuilder.add("id", SqlTypeName.INTEGER); // 整型，主键
        allTypesBuilder.add("tiny_int_col", SqlTypeName.TINYINT); // 微整型
        allTypesBuilder.add("small_int_col", SqlTypeName.SMALLINT); // 小整型
        allTypesBuilder.add("big_int_col", SqlTypeName.BIGINT); // 大整型

        // 浮点类型
        allTypesBuilder.add("float_col", SqlTypeName.FLOAT); // 浮点型
        allTypesBuilder.add("double_col", SqlTypeName.DOUBLE); // 双精度浮点型
        allTypesBuilder.add("decimal_col", typeFactory.createSqlType(SqlTypeName.DECIMAL, 10, 2)); // 定点数，总长10位，小数2位

        // 字符串类型
        allTypesBuilder.add("varchar_col", typeFactory.createSqlType(SqlTypeName.VARCHAR, 255)); // 可变长字符串
        allTypesBuilder.add("char_col", typeFactory.createSqlType(SqlTypeName.CHAR, 10)); // 定长字符串

        // 日期时间类型
        allTypesBuilder.add("date_col", SqlTypeName.DATE); // 日期
        allTypesBuilder.add("time_col", SqlTypeName.TIME); // 时间
        allTypesBuilder.add("timestamp_col", SqlTypeName.TIMESTAMP); // 时间戳

        // 布尔类型
        allTypesBuilder.add("boolean_col", SqlTypeName.BOOLEAN); // 布尔类型

        // 二进制类型
        allTypesBuilder.add("binary_col", typeFactory.createSqlType(SqlTypeName.BINARY, 100)); // 二进制数据
        allTypesBuilder.add("varbinary_col", typeFactory.createSqlType(SqlTypeName.VARBINARY, 100)); // 可变长二进制数据

        // 其他类型
        allTypesBuilder.add("null_col", SqlTypeName.NULL); // 空值
        allTypesBuilder.add("any_col", SqlTypeName.ANY); // 任意类型

        RelDataType allTypesRowType = allTypesBuilder.build();

        // 添加表到schema
        Table allTypesTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return allTypesRowType;
            }
        };
        schema.add("t_all_types", allTypesTable);

        // 3. 创建RelBuilder
        FrameworkConfig config = Frameworks.newConfigBuilder()
                .defaultSchema(schema)
                .build();

        // 4. 输出表结构信息
        printTableDefinitions(config);

        // 5. 生成CREATE TABLE语句
        String createTableSql = generateCreateTableSql("t_all_types", allTypesRowType);
        System.out.println("CREATE TABLE语句:\n" + createTableSql);

        // 6. 构建查询
        RelBuilder builder = RelBuilder.create(config);

        // SELECT id, varchar_col, date_col, boolean_col
        // FROM t_all_types
        // WHERE decimal_col > 100.0 AND boolean_col = TRUE
        // ORDER BY id DESC

        builder.scan("t_all_types");

        // 构建条件：decimal_col > 100.0 AND boolean_col = TRUE
        RexNode decimalCondition = builder.call(
                SqlStdOperatorTable.GREATER_THAN,
                builder.field("decimal_col"),
                builder.literal(100.0));

        RexNode booleanCondition = builder.equals(
                builder.field("boolean_col"),
                builder.literal(true));

        builder.filter(
                builder.and(decimalCondition, booleanCondition));

        // 选择字段
        builder.project(
                builder.field("id"),
                builder.field("varchar_col"),
                builder.field("date_col"),
                builder.field("boolean_col"));

        // 排序
        builder.sort(
                builder.desc(builder.field("id")));

        // 7. 生成SQL
        RelNode relNode = builder.build();
        SqlDialect dialect = SqlDialect.DatabaseProduct.MYSQL.getDialect();
        RelToSqlConverter converter = new RelToSqlConverter(dialect);
        SqlNode sqlNode = converter.visitRoot(relNode).asStatement();
        String sql = sqlNode.toSqlString(dialect).getSql();

        // 去掉换行符，保持SQL在一行内显示
        sql = sql.replaceAll("[\r\n]+", " ");

        System.out.println("包含所有类型的查询SQL:\n" + sql);

        // 8. 验证生成的SQL
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM `t_all_types`"));
        assertTrue(sql.contains("`decimal_col` > 100"));
        assertTrue(sql.contains("`boolean_col` = TRUE"));
        assertTrue(sql.contains("ORDER BY `id` DESC"));
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
                    if (field.getType().getPrecision() > 0) {
                        int doublePrec = field.getType().getPrecision();
                        int doubleScale = field.getType().getScale();
                        columnDef.append("DOUBLE(").append(doublePrec).append(", ").append(doubleScale).append(")");
                    } else {
                        columnDef.append("DOUBLE");
                    }
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
                case BINARY:
                    columnDef.append("BINARY(").append(field.getType().getPrecision()).append(")");
                    break;
                case VARBINARY:
                    columnDef.append("VARBINARY(").append(field.getType().getPrecision()).append(")");
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

            for (FilterCondition condition : conditions) {
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