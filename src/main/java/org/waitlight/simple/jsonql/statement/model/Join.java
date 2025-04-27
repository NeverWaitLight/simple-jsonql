package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Join {
    private String type;
    private String table;
    private WhereCondition on;
} 