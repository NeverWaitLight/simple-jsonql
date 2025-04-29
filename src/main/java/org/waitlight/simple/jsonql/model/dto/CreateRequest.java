package org.waitlight.simple.jsonql.model.dto;

import java.util.List;

/**
 * 数据创建请求
 */
public class CreateRequest {
    
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
     * 字段数组
     */
    private List<Field> fields;

    public CreateRequest() {
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

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    /**
     * 字段定义
     */
    public static class Field {
        /**
         * 字段名称
         */
        private String field;
        
        /**
         * 字段值，单值字段使用
         */
        private Object value;
        
        /**
         * 字段值数组，多值字段使用
         */
        private List<Object> values;

        public Field() {
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
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
    }
} 