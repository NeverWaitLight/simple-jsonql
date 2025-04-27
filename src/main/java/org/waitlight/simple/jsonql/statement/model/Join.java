package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Join implements Clause {
    private JoinType type;
    private String table;
    private WhereCondition on;
}
