package io.github.dhoondlayai.artifact.json.databind;

import io.github.dhoondlayai.artifact.json.annotation.*;
import io.github.dhoondlayai.artifact.json.exception.JsonMappingException;
import io.github.dhoondlayai.artifact.json.model.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * <h2>CustomObjectMapper â€” Bidirectional POJO â†” JsonNode Mapper</h2>
 *
 * <p>
 * Maps Java objects to {@link JsonNode} trees and back, with full support for:
 * </p>
 * <ul>
 * <li>{@link JsonProperty} â€” rename fields in the JSON output</li>
 * <li>{@link JsonIgnore} â€” skip fields entirely</li>
 * <li>{@link JsonNaming} â€” apply SNAKE_CASE / KEBAB_CASE / PASCAL_CASE
 * automatically</li>
 * <li>{@link TypeAdapter} â€” plug in custom serialization logic per type</li>
 * <li>Collections ({@link java.util.List}, {@link java.util.Set}) â†’
 * {@link JsonArray}</li>
 * <li>Maps â†’ {@link JsonObject}</li>
 * <li>Deep diff between two {@link JsonNode} trees</li>
 * </ul>
 *
 * <h3>Examples:</h3>
 * 
 * <pre>{@code
 * CustomObjectMapper mapper = new CustomObjectMapper();
 *
 * // Serialize POJO â†’ JsonNode
 * JsonNode json = mapper.serialize(myObject);
 *
 * // Deserialize JsonNode â†’ POJO
 * MyClass obj = mapper.deserialize(json, MyClass.class);
 *
 * // Custom adapter
 * mapper.registerAdapter(LocalDate.class, date -> new JsonValue(date.toString()));
 *
 * // Deep diff
 * JsonNode patch = mapper.diff(old, updated);
 * }</pre>
 *
 * @author artifact-json
 * @version 2.0
 */
public class CustomObjectMapper {

    private final Map<Class<?>, TypeAdapter<?>> adapters = new HashMap<>();

    /** Registers a custom {@link TypeAdapter} for the given class. */
    public <T> void registerAdapter(Class<T> clazz, TypeAdapter<T> adapter) {
        adapters.put(clazz, adapter);
    }

    //
    // Serialize: Java Object â†’ JsonNode
    //

    /**
     * Converts a Java object to a {@link JsonNode} tree.
     * Handles primitives, Strings, Collections, Maps, and arbitrary POJOs via
     * reflection.
     *
     * @param obj the object to serialize
     * @return equivalent {@link JsonNode}
     * @throws JsonMappingException on reflection or adapter errors
     */
    public JsonNode serialize(Object obj) {
        try {
            return doSerialize(obj);
        } catch (JsonMappingException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonMappingException("Serialization failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private JsonNode doSerialize(Object obj) throws Exception {
        if (obj == null)
            return new JsonValue(null);

        Class<?> clazz = obj.getClass();

        // Custom adapter
        if (adapters.containsKey(clazz)) {
            TypeAdapter<Object> adapter = (TypeAdapter<Object>) adapters.get(clazz);
            return adapter.toJson(obj);
        }

        // Primitives & strings
        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
            return new JsonValue(obj);
        }

        // Collections â†’ JsonArray
        if (obj instanceof Collection<?> coll) {
            JsonArray arr = new JsonArray(coll.size());
            for (Object item : coll)
                arr.add(doSerialize(item));
            return arr;
        }

        // Maps â†’ JsonObject
        if (obj instanceof Map<?, ?> map) {
            JsonObject jsonObj = new JsonObject(map.size());
            for (var entry : map.entrySet()) {
                jsonObj.put(String.valueOf(entry.getKey()), doSerialize(entry.getValue()));
            }
            return jsonObj;
        }

        // Arrays
        if (clazz.isArray()) {
            int len = Array.getLength(obj);
            JsonArray arr = new JsonArray(len);
            for (int i = 0; i < len; i++)
                arr.add(doSerialize(Array.get(obj, i)));
            return arr;
        }

        // POJO via reflection
        JsonObject jsonObj = new JsonObject();
        JsonNaming naming = clazz.getAnnotation(JsonNaming.class);
        for (Field field : getAllFields(clazz)) {
            if (field.isAnnotationPresent(JsonIgnore.class))
                continue;
            field.setAccessible(true);
            String key = resolvePropertyName(field, naming);
            jsonObj.put(key, doSerialize(field.get(obj)));
        }
        return jsonObj;
    }

    //
    // Deserialize: JsonNode â†’ Java Object
    //

    /**
     * Converts a {@link JsonNode} to a POJO of the given class.
     *
     * @param node  the JSON tree
     * @param clazz the target class
     * @param <T>   target type
     * @return populated instance of {@code clazz}
     * @throws JsonMappingException on failures
     */
    public <T> T deserialize(JsonNode node, Class<T> clazz) {
        try {
            return doDeserialize(node, clazz);
        } catch (JsonMappingException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonMappingException("Deserialization to " + clazz.getSimpleName()
                    + " failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T doDeserialize(JsonNode node, Class<T> clazz) throws Exception {
        if (node instanceof JsonValue val) {
            Object raw = val.value();
            if (raw == null)
                return null;
            // Type coercion
            if (clazz == String.class)
                return clazz.cast(String.valueOf(raw));
            if (clazz == int.class || clazz == Integer.class)
                return (T) (Integer) ((Number) raw).intValue();
            if (clazz == long.class || clazz == Long.class)
                return (T) (Long) ((Number) raw).longValue();
            if (clazz == double.class || clazz == Double.class)
                return (T) (Double) ((Number) raw).doubleValue();
            if (clazz == boolean.class || clazz == Boolean.class)
                return (T) (Boolean) raw;
            return clazz.cast(raw);
        }

        if (!(node instanceof JsonObject jsonObj)) {
            throw new JsonMappingException("Cannot deserialize " + node.getClass().getSimpleName()
                    + " to " + clazz.getSimpleName());
        }

        T instance;
        try {
            instance = clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new JsonMappingException("Class " + clazz.getSimpleName()
                    + " must have a public no-arg constructor", e);
        }

        JsonNaming naming = clazz.getAnnotation(JsonNaming.class);
        for (Field field : getAllFields(clazz)) {
            if (field.isAnnotationPresent(JsonIgnore.class))
                continue;
            field.setAccessible(true);
            String propName = resolvePropertyName(field, naming);
            JsonNode fieldNode = jsonObj.field(propName);

            if (fieldNode != null) {
                try {
                    field.set(instance, doDeserialize(fieldNode, field.getType()));
                } catch (Exception e) {
                    throw new JsonMappingException("Failed to set field", propName, field.getType());
                }
            } else if (field.isAnnotationPresent(JsonProperty.class)
                    && field.getAnnotation(JsonProperty.class).required()) {
                throw new JsonMappingException("Missing required field", propName, field.getType());
            }
        }
        return instance;
    }

    //
    // Deep Diff
    //

    /**
     * Produces a {@link JsonObject} describing the differences between
     * {@code source}
     * and {@code target}. Only modified or added fields are included.
     * An empty result means the two nodes are identical.
     *
     * @param source original node
     * @param target updated node
     * @return diff as a {@link JsonNode}
     */
    public JsonNode diff(JsonNode source, JsonNode target) {
        if (source.equals(target))
            return new JsonObject();

        if (source instanceof JsonObject sObj && target instanceof JsonObject tObj) {
            JsonObject diff = new JsonObject();
            tObj.fields().forEach((k, v) -> {
                JsonNode sVal = sObj.field(k);
                if (sVal == null) {
                    diff.put(k, v); // added
                } else if (!sVal.equals(v)) {
                    diff.put(k, diff(sVal, v)); // changed (recurse)
                }
            });
            return diff;
        }
        return target;
    }

    //
    // Helpers
    //

    private String resolvePropertyName(Field field, JsonNaming naming) {
        if (field.isAnnotationPresent(JsonProperty.class)) {
            String val = field.getAnnotation(JsonProperty.class).value();
            if (!val.isEmpty())
                return val;
        }
        String name = field.getName();
        if (naming == null)
            return name;
        return switch (naming.value()) {
            case SNAKE_CASE -> name.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
            case KEBAB_CASE -> name.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase();
            case PASCAL_CASE -> Character.toUpperCase(name.charAt(0)) + name.substring(1);
            default -> name;
        };
    }

    /** Collects declared fields from the class and all its superclasses. */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
            c = c.getSuperclass();
        }
        return fields;
    }
}
