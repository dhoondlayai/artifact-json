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
    private static final int MAX_DEPTH = 100; // Security: prevent DOS via deep recursion

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
            return doSerialize(obj, 0);
        } catch (JsonMappingException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonMappingException("Serialization failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private JsonNode doSerialize(Object obj, int depth) throws Exception {
        if (depth > MAX_DEPTH) {
            throw new JsonMappingException("Serialization depth exceeded limit of " + MAX_DEPTH + ". Possible circular reference?");
        }
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
                arr.add(doSerialize(item, depth + 1));
            return arr;
        }

        // Maps â†’ JsonObject
        if (obj instanceof Map<?, ?> map) {
            JsonObject jsonObj = new JsonObject(map.size());
            for (var entry : map.entrySet()) {
                jsonObj.put(String.valueOf(entry.getKey()), doSerialize(entry.getValue(), depth + 1));
            }
            return jsonObj;
        }

        // Arrays
        if (clazz.isArray()) {
            int len = Array.getLength(obj);
            JsonArray arr = new JsonArray(len);
            for (int i = 0; i < len; i++)
                arr.add(doSerialize(Array.get(obj, i), depth + 1));
            return arr;
        }

        // POJO via reflection
        JsonObject jsonObj = new JsonObject();
        JsonNaming naming = clazz.getAnnotation(JsonNaming.class);
        JsonInclude classInclude = clazz.getAnnotation(JsonInclude.class);
        for (Field field : getAllFields(clazz)) {
            if (field.isAnnotationPresent(JsonIgnore.class))
                continue;
            // @JsonWriteOnly: accepted on input, never emitted in output
            if (field.isAnnotationPresent(JsonWriteOnly.class))
                continue;
            field.setAccessible(true);
            String key = resolvePropertyName(field, naming);
            Object rawValue = field.get(obj);

            // @JsonUnwrapped support
            if (field.isAnnotationPresent(JsonUnwrapped.class) && rawValue != null) {
                JsonUnwrapped unwrapped = field.getAnnotation(JsonUnwrapped.class);
                JsonNode unwrappedNode = doSerialize(rawValue, depth + 1);
                if (unwrappedNode instanceof JsonObject unwrappedObj) {
                    unwrappedObj.fields().forEach((uk, uv) -> {
                        jsonObj.put(unwrapped.prefix() + uk + unwrapped.suffix(), uv);
                    });
                }
                continue;
            }

            JsonNode valueNode;
            if (field.isAnnotationPresent(PII.class)) {
                PII pii = field.getAnnotation(PII.class);
                String maskStr = !pii.mask().equals("****") ? pii.mask()
                              : !pii.value().isEmpty()       ? pii.value()
                              : "****";
                valueNode = new JsonValue(maskStr);
            } else {
                valueNode = doSerialize(rawValue, depth + 1);
            }
            // @JsonInclude filtering (field-level overrides class-level)
            JsonInclude inc = field.isAnnotationPresent(JsonInclude.class)
                    ? field.getAnnotation(JsonInclude.class) : classInclude;
            if (inc != null && shouldExclude(inc.value(), valueNode, rawValue))
                continue;
            jsonObj.put(key, valueNode);
        }
        
        // @JsonVirtual support
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(JsonVirtual.class) && method.getParameterCount() == 0) {
                method.setAccessible(true);
                String name = method.getName().startsWith("get") ? method.getName().substring(3) : method.getName();
                jsonObj.put(name, doSerialize(method.invoke(obj), depth + 1));
            }
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
            return doDeserialize(node, clazz, 0);
        } catch (JsonMappingException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonMappingException("Deserialization to " + clazz.getSimpleName()
                    + " failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T doDeserialize(JsonNode node, Class<T> clazz, int depth) throws Exception {
        if (depth > MAX_DEPTH) {
            throw new JsonMappingException("Deserialization depth exceeded limit of " + MAX_DEPTH);
        }
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
            // Never deserialize PII or ReadOnly fields from the wire
            if (field.isAnnotationPresent(PII.class) || field.isAnnotationPresent(JsonReadOnly.class))
                continue;
            field.setAccessible(true);
            String propName = resolvePropertyName(field, naming);
            JsonNode fieldNode = jsonObj.field(propName);

            // @JsonAlias: try alternative names if primary key is absent
            if (fieldNode == null && field.isAnnotationPresent(JsonAlias.class)) {
                for (String alias : field.getAnnotation(JsonAlias.class).value()) {
                    fieldNode = jsonObj.field(alias);
                    if (fieldNode != null) break;
                }
            }

            // @JsonProperty.required check is already here, keeping it for backward compat
            if (fieldNode != null) {
                try {
                    Object deserialized = doDeserialize(fieldNode, field.getType(), depth + 1);
                    field.set(instance, deserialized);
                } catch (Exception e) {
                    throw new JsonMappingException("Failed to set field", propName, field.getType());
                }
            } else if (field.isAnnotationPresent(JsonDefault.class)) {
                // @JsonDefault: apply fallback value when key is absent
                String defaultStr = field.getAnnotation(JsonDefault.class).value();
                try {
                    field.set(instance, doDeserialize(new JsonValue(coerceDefault(defaultStr, field.getType())), field.getType(), depth + 1));
                } catch (Exception e) { /* leave field at Java default */ }
            } else if (field.isAnnotationPresent(JsonProperty.class)
                    && field.getAnnotation(JsonProperty.class).required()) {
                throw new JsonMappingException("Missing required field", propName, field.getType());
            }
            
            // @JsonValidate logic
            if (field.isAnnotationPresent(JsonValidate.class)) {
                validateField(field, instance, propName);
            }
        }
        return instance;
    }

    private void validateField(Field field, Object instance, String propName) throws Exception {
        JsonValidate v = field.getAnnotation(JsonValidate.class);
        Object val = field.get(instance);
        
        if (v.required() && val == null) {
            throw new JsonMappingException("Validation failed: field '" + propName + "' is required");
        }
        
        if (val instanceof String s && !v.regex().isEmpty()) {
            if (!s.matches(v.regex())) {
                throw new JsonMappingException("Validation failed: field '" + propName + "' does not match regex " + v.regex());
            }
        }
        
        if (val instanceof Number n) {
            double d = n.doubleValue();
            if (d < v.min() || d > v.max()) {
                throw new JsonMappingException("Validation failed: field '" + propName + "' outside range [" + v.min() + ", " + v.max() + "]");
            }
        }
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

    /**
     * Returns {@code true} if the field should be excluded based on the
     * {@link JsonInclude} strategy.
     */
    private boolean shouldExclude(JsonInclude.Include strategy, JsonNode node, Object raw) {
        if (strategy == JsonInclude.Include.NON_NULL) {
            return node instanceof JsonValue v && v.value() == null;
        } else if (strategy == JsonInclude.Include.NON_EMPTY) {
            if (node instanceof JsonValue v && v.value() == null) return true;
            if (raw instanceof String s) return s.isEmpty();
            if (raw instanceof Collection<?> c) return c.isEmpty();
            if (raw instanceof Map<?, ?> m) return m.isEmpty();
            if (node instanceof JsonArray a) return a.isEmpty();
            if (node instanceof JsonObject o) return o.isEmpty();
            return false;
        } else if (strategy == JsonInclude.Include.NON_DEFAULT) {
            if (node instanceof JsonValue v) {
                Object val = v.value();
                return val == null
                    || (val instanceof Number n && n.doubleValue() == 0.0)
                    || (val instanceof Boolean b && !b)
                    || (val instanceof String s && s.isEmpty());
            }
            return false;
        }
        return false; // ALWAYS
    }

    /** Coerce a @JsonDefault string to the field's raw type for the deserializer. */
    private Object coerceDefault(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        return value;
    }
}
