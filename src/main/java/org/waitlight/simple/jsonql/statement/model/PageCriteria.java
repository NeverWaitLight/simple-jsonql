package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageCriteria {
    private Integer size;
    private Integer number;
}