package com.phrase.custom.adapter.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SmartMasker {

    private static final Set<String> SENSITIVE_KEYS = Set.of("password", "secret", "key", "token", "auth", "credential");

    public static String mask(Object obj) {
        return traverse(obj, new HashSet<>());
    }

    private static String traverse(Object obj, Set<Object> visited) {
        if (obj == null) return "null";

        // Prevent infinite recursion for circular references
        if (visited.contains(obj) && !isPrimitiveOrWrapper(obj)) return "[Circular]";
        if (!isPrimitiveOrWrapper(obj)) visited.add(obj);

        // 1. Handle Maps
        if (obj instanceof Map<?, ?> map) {
            return "{" + map.entrySet().stream()
                    .map(entry -> {
                        String key = String.valueOf(entry.getKey());
                        Object value = entry.getValue();
                        return key + "=" + (isSensitive(key) ? "*******" : traverse(value, visited));
                    })
                    .collect(Collectors.joining(", ")) + "}";
        }

        // 2. Handle Collections/Arrays
        if (obj instanceof Iterable<?> iterable) {
            List<String> parts = new ArrayList<>();
            for (Object item : iterable) parts.add(traverse(item, visited));
            return "[" + String.join(", ", parts) + "]";
        }

        // 3. Handle Custom Objects via Reflection
        if (obj.getClass().getName().startsWith("java.")) return String.valueOf(obj);

        Field[] fields = obj.getClass().getDeclaredFields();
        return obj.getClass().getSimpleName() + "[" +
                Arrays.stream(fields)
                        .map(field -> {
                            field.setAccessible(true);
                            try {
                                String name = field.getName();
                                Object value = field.get(obj);
                                return name + "=" + (isSensitive(name) ? "*******" : traverse(value, visited));
                            } catch (Exception e) {
                                return field.getName() + "=ERR";
                            }
                        })
                        .collect(Collectors.joining(", ")) + "]";
    }

    private static boolean isSensitive(String key) {
        if (key == null) return false;
        String lowerKey = key.toLowerCase();
        return SENSITIVE_KEYS.stream().anyMatch(lowerKey::contains);
    }

    private static boolean isPrimitiveOrWrapper(Object obj) {
        return obj instanceof String || obj instanceof Number || obj instanceof Boolean || obj.getClass().isPrimitive();
    }
}