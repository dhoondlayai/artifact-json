package io.github.dhoondlayai.artifact.json.databind;

import io.github.dhoondlayai.artifact.json.annotation.*;
import io.github.dhoondlayai.artifact.json.exception.JsonMappingException;
import io.github.dhoondlayai.artifact.json.model.*;

import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h2>FastObjectMapper â€” High-Performance Serialization</h2>
 *
 * <p>Uses Java 17+ {@link MethodHandles} for near-native access speed.
 * Reflection is typically 2-5x slower than direct access; MethodHandles
 * compile down to native instructions by the JVM, closing that gap.</p>
 *
 * @author artifact-json
 * @version 2.0
 */
public class FastObjectMapper {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private final Map<Class<?>, Map<String, MethodHandle>> fieldGetters = new ConcurrentHashMap<>();

    /**
     * Serializes an object to a {@link JsonNode} using optimized MethodHandles.
     *
     * @param obj the Java object to serialize
     * @return {@link JsonNode} representation
     * @throws JsonMappingException if access fails
     */
    public JsonNode serialize(Object obj) {
        try {
            return doSerialize(obj);
        } catch (JsonMappingException e) {
            throw e;
        } catch (Throwable e) {
            throw new JsonMappingException("Fast serialization failed", e);
        }
    }

    private JsonNode doSerialize(Object obj) throws Throwable {
        if (obj == null) return new JsonValue(null);
        Class<?> clazz = obj.getClass();
        
        if (clazz.isPrimitive() || obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
            return new JsonValue(obj);
        }

        if (obj instanceof Collection<?> coll) {
            JsonArray arr = new JsonArray(coll.size());
            for (Object item : coll) arr.add(doSerialize(item));
            return arr;
        }

        if (obj instanceof Map<?, ?> map) {
            JsonObject jsonObj = new JsonObject(map.size());
            for (var entry : map.entrySet()) {
                jsonObj.put(String.valueOf(entry.getKey()), doSerialize(entry.getValue()));
            }
            return jsonObj;
        }

        JsonObject jsonObj = new JsonObject();
        Map<String, MethodHandle> getters = fieldGetters.computeIfAbsent(clazz, this::prepareGetters);
        
        for (Map.Entry<String, MethodHandle> entry : getters.entrySet()) {
            Object value = entry.getValue().invoke(obj);
            jsonObj.put(entry.getKey(), doSerialize(value));
        }
        return jsonObj;
    }

    private Map<String, MethodHandle> prepareGetters(Class<?> clazz) {
        Map<String, MethodHandle> map = new LinkedHashMap<>();
        JsonNaming naming = clazz.getAnnotation(JsonNaming.class);
        
        try {
            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(clazz, LOOKUP);
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(JsonIgnore.class)) {
                    continue;
                }
                String propName = resolvePropertyName(field, naming);
                map.put(propName, privateLookup.unreflectGetter(field));
            }
        } catch (IllegalAccessException e) {
            throw new JsonMappingException("Cannot create private lookup for " + clazz.getName(), e);
        }
        return map;
    }

    private String resolvePropertyName(Field field, JsonNaming naming) {
        if (field.isAnnotationPresent(JsonProperty.class)) {
            String value = field.getAnnotation(JsonProperty.class).value();
            if (!value.isEmpty()) return value;
        }
        
        String name = field.getName();
        if (naming != null) {
            return switch (naming.value()) {
                case SNAKE_CASE -> name.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
                case KEBAB_CASE -> name.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase();
                case PASCAL_CASE -> Character.toUpperCase(name.charAt(0)) + name.substring(1);
                default -> name;
            };
        }
        return name;
    }
}
