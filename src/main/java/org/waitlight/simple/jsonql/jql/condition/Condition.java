package org.waitlight.simple.jsonql.jql.condition;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ComparisonCondition.class, name = "comparison"),
        @JsonSubTypes.Type(value = LogicalCondition.class, name = "logical"),
        @JsonSubTypes.Type(value = BetweenCondition.class, name = "between"),
        @JsonSubTypes.Type(value = InCondition.class, name = "in"),
        @JsonSubTypes.Type(value = NullCondition.class, name = "null"),
        @JsonSubTypes.Type(value = LikeCondition.class, name = "like"),
        @JsonSubTypes.Type(value = ExistsCondition.class, name = "exists")
})
public abstract class Condition {
    private String type;
    public abstract String toSql();
} 