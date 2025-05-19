package org.waitlight.simple.jsonql.statement.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ComparisonCondition.class, name = "comparison"),
        @JsonSubTypes.Type(value = LogicalCondition.class, name = "logical"),
        @JsonSubTypes.Type(value = SubqueryCondition.class, name = "subquery"),
        @JsonSubTypes.Type(value = BetweenCondition.class, name = "between")
})
public interface WhereCondition extends Clause {
    ConditionType getType();
}
