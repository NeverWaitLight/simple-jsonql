package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SelectStatement extends JsonqlStatement {
    private List<String> select;
    private String from;
    private List<Join> joins;
    private WhereCondition where;
    private OrderBy orderBy;
    private Integer limit;
    private Integer offset;
} 