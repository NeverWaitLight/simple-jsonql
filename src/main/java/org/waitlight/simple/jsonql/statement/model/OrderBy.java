package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderBy implements Clause {
    private String field;
    private Direction direction;
}
