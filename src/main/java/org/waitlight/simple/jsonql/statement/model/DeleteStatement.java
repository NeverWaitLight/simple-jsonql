package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteStatement extends JsonqlStatement {
    private String from;
    private WhereCondition where;
} 