package org.waitlight.simple.jsonql.service.impl;

import org.springframework.stereotype.Service;
import org.waitlight.simple.jsonql.model.dto.*;
import org.waitlight.simple.jsonql.service.DataService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 数据操作服务实现类
 */
@Service
public class DataServiceImpl implements DataService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Map<String, Object> create(CreateRequest request) {
        // 模拟创建数据，实际应用中应该连接数据库
        Map<String, Object> result = new HashMap<>();
        result.put("id", UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        
        // 处理字段
        for (CreateRequest.Field field : request.getFields()) {
            if (field.getValue() != null) {
                result.put(field.getField(), field.getValue());
            } else if (field.getValues() != null) {
                result.put(field.getField(), field.getValues());
            }
        }
        
        // 添加系统字段
        LocalDateTime now = LocalDateTime.now();
        String currentTime = now.format(DATE_FORMATTER);
        result.put("createdBy", "admin");
        result.put("createdAt", currentTime);
        result.put("updatedBy", "admin");
        result.put("updatedAt", currentTime);
        
        return result;
    }

    @Override
    public int remove(RemoveRequest request) {
        // 模拟删除数据，返回删除的记录数
        return request.getIds().size();
    }

    @Override
    public Map<String, Object> update(UpdateRequest request) {
        // 模拟更新数据
        Map<String, Object> result = new HashMap<>();
        result.put("id", request.getDataId());
        
        // 处理字段
        for (UpdateRequest.Field field : request.getFields()) {
            if (field.getValue() != null) {
                result.put(field.getField(), field.getValue());
            } else if (field.getValues() != null) {
                result.put(field.getField(), field.getValues());
            }
        }
        
        // 添加系统字段
        LocalDateTime now = LocalDateTime.now();
        String currentTime = now.format(DATE_FORMATTER);
        result.put("createdBy", "admin");
        result.put("createdAt", "2025-01-01 12:30:11");
        result.put("updatedBy", "admin");
        result.put("updatedAt", currentTime);
        
        return result;
    }

    @Override
    public PageResult<Map<String, Object>> page(PageRequest request) {
        // 模拟查询数据
        List<Map<String, Object>> list = new ArrayList<>();
        
        // 模拟查询数据1
        Map<String, Object> item1 = new HashMap<>();
        item1.put("id", "1");
        item1.put("name", "tom");
        item1.put("createdBy", "admin");
        item1.put("createdAt", "2025-01-01 12:30:11");
        item1.put("updatedBy", "admin");
        item1.put("updatedAt", "2025-01-01 12:30:11");
        list.add(item1);
        
        // 如果有子查询
        if (request.getFilters() != null && 
            request.getFilters().getConditions().stream()
                .anyMatch(c -> c.getField() != null && c.getField().contains("blogs."))) {
            List<Map<String, Object>> blogs = new ArrayList<>();
            Map<String, Object> blog = new HashMap<>();
            blog.put("id", "321");
            blog.put("title", "活着");
            blogs.add(blog);
            item1.put("blogs", blogs);
        }
        
        long total = 1;
        int page = request.getPage() != null ? request.getPage().getNumber() : 1;
        int size = request.getPage() != null ? request.getPage().getSize() : 10;
        
        return new PageResult<>(list, total, page, size);
    }
} 