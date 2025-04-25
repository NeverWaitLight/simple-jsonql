package org.waitlight.simple.jsonql.jql;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JsonQL(
        Statement statement,
        List<String> select,
        String into,
        String update,
        Map<String, Object> set,
        LinkedHashMap<String, String> values,
        String from,
        Join join,
        Where where
) {
}