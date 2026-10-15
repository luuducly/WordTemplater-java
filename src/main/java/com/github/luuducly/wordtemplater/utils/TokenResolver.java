package com.github.luuducly.wordtemplater.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenResolver {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Object resolve(Object data, String path) {
        if (data == null || path == null || path.trim().isEmpty()) {
            return null;
        }

        Pattern pattern = Pattern.compile("([^[.\\]]+)|\\[(\\d+)\\]");
        Matcher matcher = pattern.matcher(path.trim());
        List<String> parts = new ArrayList<>();

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                parts.add(matcher.group(1));
            } else if (matcher.group(2) != null) {
                parts.add(matcher.group(2));
            }
        }

        Object current = data;
        for (String part : parts) {
            if (current == null) return null;

            if (current instanceof JsonNode) {
                JsonNode jNode = (JsonNode) current;
                if (jNode.isArray()) {
                    try {
                        int idx = Integer.parseInt(part);
                        current = jNode.get(idx);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                } else if (jNode.isObject()) {
                    JsonNode child = jNode.get(part);
                    if (child == null) {
                        // Case-insensitive search
                        String lowerPart = part.toLowerCase();
                        var it = jNode.fieldNames();
                        while (it.hasNext()) {
                            String key = it.next();
                            if (key.equalsIgnoreCase(lowerPart)) {
                                child = jNode.get(key);
                                break;
                            }
                        }
                    }
                    current = child;
                } else {
                    return null;
                }
                continue;
            }

            if (current instanceof List) {
                try {
                    int idx = Integer.parseInt(part);
                    List<?> list = (List<?>) current;
                    current = (idx >= 0 && idx < list.size()) ? list.get(idx) : null;
                } catch (NumberFormatException e) {
                    return null;
                }
                continue;
            }

            if (current instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) current;
                if (map.containsKey(part)) {
                    current = map.get(part);
                } else {
                    String lowerPart = part.toLowerCase();
                    Object found = null;
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (String.valueOf(entry.getKey()).equalsIgnoreCase(lowerPart)) {
                            found = entry.getValue();
                            break;
                        }
                    }
                    current = found;
                }
                continue;
            }

            // POCO / Java Bean reflection
            current = resolveProperty(current, part);
        }

        if (current instanceof JsonNode) {
            JsonNode jNode = (JsonNode) current;
            if (jNode.isValueNode()) {
                if (jNode.isTextual()) return jNode.asText();
                if (jNode.isNumber()) return jNode.numberValue();
                if (jNode.isBoolean()) return jNode.asBoolean();
                if (jNode.isNull()) return null;
            }
        }

        return current;
    }

    private static Object resolveProperty(Object obj, String propName) {
        Class<?> clazz = obj.getClass();
        String getterName = "get" + Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
        String isName = "is" + Character.toUpperCase(propName.charAt(0)) + propName.substring(1);

        for (Method method : clazz.getMethods()) {
            if (method.getParameterCount() == 0) {
                if (method.getName().equalsIgnoreCase(getterName) || method.getName().equalsIgnoreCase(isName) || method.getName().equalsIgnoreCase(propName)) {
                    try {
                        return method.invoke(obj);
                    } catch (Exception ignored) {}
                }
            }
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equalsIgnoreCase(propName)) {
                try {
                    field.setAccessible(true);
                    return field.get(obj);
                } catch (Exception ignored) {}
            }
        }

        return null;
    }
}
