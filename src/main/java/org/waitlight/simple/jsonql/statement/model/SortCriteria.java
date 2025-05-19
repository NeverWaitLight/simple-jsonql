package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SortCriteria {
    private String field;
    private DirectionType direction;
}