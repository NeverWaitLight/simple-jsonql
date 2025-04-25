package org.waitlight.simple.jsonql.jql;

import java.util.LinkedHashMap;
import java.util.List;

public record JsonQL(
        Statement statement,
        List<String> select,
        String into,
        LinkedHashMap<String, String> values,
        String from,
        Join join,
        Where where
) {
}