package org.waitlight.simple.jsonql.builder;

import org.waitlight.simple.jsonql.statement.JsonQLStatement;
import org.waitlight.simple.jsonql.statement.model.PersistStatement;

import java.util.List;

public interface SqlBuilder<T extends JsonQLStatement> {
    /**
     * 通过 {@link JsonQLStatement} 构建 sql
     *
     * @param statement 待处理的语句
     * @return 预处理后的 sql
     * @throws SqlBuildException 构建异常
     */
    PreparedSql<T> build(T statement) throws SqlBuildException;

    /**
     * 转换为单层statement集合
     *
     * @param statement 待处理的语句
     * @return 嵌套语句
     */
    List<PersistStatement> convert2SingleLayer(T statement);
}
