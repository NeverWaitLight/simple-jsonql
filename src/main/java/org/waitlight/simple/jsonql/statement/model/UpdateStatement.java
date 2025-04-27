package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class UpdateStatement extends JsonqlStatement {
    private String update;
    private Map<String, Object> set;
    private WhereCondition where;
} 