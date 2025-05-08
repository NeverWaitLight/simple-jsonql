package org.waitlight.simple.jsonql.engine;

import lombok.Data;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
public class PreparedSql<T extends JsonQLStatement> {
    private String sql;
    private List<Object> parameters;
    private Class<T> statementType;
    private List<PreparedSql<T>> nestedSQLs = new ArrayList<>();

    /**
     * 默认构造函数
     */
    public PreparedSql() {
    }

    /**
     * 包含SQL语句的构造函数
     *
     * @param sql SQL语句
     */
    public PreparedSql(String sql) {
        this.sql = sql;
        this.parameters = new ArrayList<>();
    }

    /**
     * 包含SQL语句和参数的构造函数
     *
     * @param sql        SQL语句
     * @param parameters SQL参数列表
     */
    public PreparedSql(String sql, List<Object> parameters) {
        this.sql = sql;
        this.parameters = parameters;
    }

    /**
     * 完整构造函数
     *
     * @param sql           SQL语句
     * @param parameters    SQL参数列表
     * @param statementType SQL语句类型
     */
    public PreparedSql(String sql, List<Object> parameters, Class<T> statementType) {
        this.sql = sql;
        this.parameters = parameters;
        this.statementType = statementType;
    }

    /**
     * 添加嵌套SQL
     *
     * @param nestedSql 要添加的嵌套SQL
     */
    public void addNestedSQLs(PreparedSql<T> nestedSql) {
        this.nestedSQLs.add(nestedSql);
    }

    /**
     * 检查SQL语句是否为空
     *
     * @return 如果SQL语句为空则返回true, 否则返回false
     */
    public boolean isEmpty() {
        return sql == null || sql.isEmpty();
    }

    /**
     * 检查SQL语句是否不为空
     *
     * @return 如果SQL语句不为空则返回true, 否则返回false
     */
    public boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     * 获取参数列表，如果为空则返回空列表
     *
     * @return 参数列表，不会返回null
     */
    public List<Object> getParameters() {
        return parameters == null ? Collections.emptyList() : parameters;
    }
}