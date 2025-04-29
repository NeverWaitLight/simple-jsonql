package org.waitlight.simple.jsonql.statement.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * WHERE 条件接口
 * 所有条件类型都必须实现此接口
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ComparisonCondition.class, name = "comparison"),
    @JsonSubTypes.Type(value = LogicalCondition.class, name = "logical"),
    @JsonSubTypes.Type(value = SubqueryCondition.class, name = "subquery"),
    @JsonSubTypes.Type(value = BetweenCondition.class, name = "between")
})
public interface WhereCondition extends Clause {
    /**
     * 获取条件类型
     *
     * @return 条件类型枚举
     */
    ConditionType getType();
}
