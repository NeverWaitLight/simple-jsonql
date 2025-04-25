package org.waitlight.simple.jsonql;

import java.util.HashMap;
import java.util.Map;

public class BaseModel {
    public Map<String, Object> originalValues = new HashMap<>();
    public Map<String, Object> currentValues = new HashMap<>();
    public Map<String, Field> schema = new HashMap<>();
    public Map<String, Relationship> relationships;
    public String status = "new"; // new, loaded, dirty
}
