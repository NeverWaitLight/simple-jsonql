package org.waitlight.simple.jsonql;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface Repository<T> {
    // 基础CRUD操作
    CompletableFuture<T> create(Map<String, Object> data);
    CompletableFuture<T> find(Map<String, Object> criteria);
    CompletableFuture<Collection<T>> findAll(Map<String, Object> criteria);
    CompletableFuture<Boolean> update(T model);
    CompletableFuture<Boolean> delete(T model);

    // 增强查询方法
    CompletableFuture<Collection<T>> findAll(Map<String, Object> criteria, int offset, int limit);
    CompletableFuture<Collection<T>> findAll(Map<String, Object> criteria, String sortField, boolean ascending);
    CompletableFuture<Collection<T>> findAll(Map<String, Object> criteria, int offset, int limit, String sortField, boolean ascending);

    // 批量操作
    CompletableFuture<Collection<T>> batchCreate(Collection<Map<String, Object>> dataList);
    CompletableFuture<Boolean> batchUpdate(Collection<T> models);
    CompletableFuture<Boolean> batchDelete(Collection<T> models);

    // 关系查询
    CompletableFuture<Collection<T>> getRelated(String relationshipName, Map<String, Object> criteria);
    CompletableFuture<Collection<T>> getRelated(String relationshipName, Map<String, Object> criteria, int offset, int limit);

    // 事务支持
    CompletableFuture<Void> executeInTransaction(Runnable operation);
}
