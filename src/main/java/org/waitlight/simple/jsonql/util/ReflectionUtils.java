package org.waitlight.simple.jsonql.util;

import org.waitlight.simple.jsonql.metadata.MetadataException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

public class ReflectionUtils {
    public static Class<?> getGenericType(Field field) {
        Type genericType = field.getGenericType();

        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();

            Class<?> x = (Class<?>) Arrays.stream(typeArguments)
                    .filter(item -> item instanceof Class)
                    .findFirst()
                    .orElseThrow(() -> new MetadataException("Field: " + field.getName() + " has no generic type"));
            return x;
        }
        throw new MetadataException("Field: " + field.getName() + " has no generic type");
    }
}
