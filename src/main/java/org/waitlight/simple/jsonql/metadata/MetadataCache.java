package org.waitlight.simple.jsonql.metadata;

public interface MetadataCache<T, R> {
    void add(T t);

    R get(String entityName);
}
