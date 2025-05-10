package org.waitlight.simple.jsonql.model.dto;

import java.util.List;

/**
 * 分页查询请求
 */
public class PageRequest {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 表单ID
     */
    private String formId;

    /**
     * 实体ID
     */
    private String entityId;

    /**
     * 过滤条件
     */
    private Filter filters;

    /**
     * 排序条件
     */
    private List<Sort> sort;

    /**
     * 分页信息
     */
    private Page page;

    public PageRequest() {
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Filter getFilters() {
        return filters;
    }

    public void setFilters(Filter filters) {
        this.filters = filters;
    }

    public List<Sort> getSort() {
        return sort;
    }

    public void setSort(List<Sort> sort) {
        this.sort = sort;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    /**
     * 过滤条件
     */
    public static class Filter {
        /**
         * 条件关系，"and"或"or"
         */
        private String rel;

        /**
         * 条件数组
         */
        private List<Condition> conditions;

        public Filter() {
        }

        public String getRel() {
            return rel;
        }

        public void setRel(String rel) {
            this.rel = rel;
        }

        public List<Condition> getConditions() {
            return conditions;
        }

        public void setConditions(List<Condition> conditions) {
            this.conditions = conditions;
        }
    }

    /**
     * 过滤条件项
     */
    public static class Condition {
        /**
         * 字段名称
         */
        private String field;

        /**
         * 匹配方法，如"eq"、"in"、"like"等
         */
        private String method;

        /**
         * 匹配值，单值匹配时使用
         */
        private Object value;

        /**
         * 匹配值数组，多值匹配时使用
         */
        private List<Object> values;

        /**
         * 关系过滤条件，用于嵌套条件
         */
        private Filter relation;

        public Condition() {
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public List<Object> getValues() {
            return values;
        }

        public void setValues(List<Object> values) {
            this.values = values;
        }

        public Filter getRelation() {
            return relation;
        }

        public void setRelation(Filter relation) {
            this.relation = relation;
        }
    }

    /**
     * 排序条件
     */
    public static class Sort {
        /**
         * 排序字段
         */
        private String field;

        /**
         * 排序方向，"ASC"或"DESC"
         */
        private String direction;

        public Sort() {
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }
    }

    /**
     * 分页信息
     */
    public static class Page {
        /**
         * 每页记录数
         */
        private int size;

        /**
         * 页码，从1开始
         */
        private int number;

        public Page() {
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }
    }
} 