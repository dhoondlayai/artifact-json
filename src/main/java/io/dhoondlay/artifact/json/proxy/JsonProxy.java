package io.dhoondlay.artifact.json.proxy;

import io.dhoondlay.artifact.json.model.*;
import io.dhoondlay.artifact.json.annotation.JsonProperty;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 🚀 Zero-Cost Deserialization using Dynamic Proxies.
 * Instead of mapping a JSON tree into a heavy POJO, this creates a lightweight
 * Java Interface Proxy that reads/writes directly to the underlying JsonNode.
 * 
 * Perfect for massive configurations or read-heavy APIs where full deserialization is a waste.
 */
public class JsonProxy {

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> interfaceType, JsonObject backingNode) {
        return (T) Proxy.newProxyInstance(
            interfaceType.getClassLoader(),
            new Class<?>[]{interfaceType},
            new JsonInvocationHandler(backingNode)
        );
    }

    private static class JsonInvocationHandler implements InvocationHandler {
        private final JsonObject backingNode;

        public JsonInvocationHandler(JsonObject backingNode) {
            this.backingNode = backingNode;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            String propName = getPropertyName(method);

            if (methodName.startsWith("get") || methodName.startsWith("is")) {
                JsonNode val = backingNode.field(propName);
                if (val instanceof JsonValue v) {
                    return castValue(v.value(), method.getReturnType());
                } else if (val instanceof JsonObject obj && method.getReturnType().isInterface()) {
                    return create(method.getReturnType(), obj); // Nested proxies!
                }
                return null;
            } else if (methodName.startsWith("set") && args != null && args.length == 1) {
                Object arg = args[0];
                if (arg == null) {
                    backingNode.put(propName, new JsonValue(null));
                } else if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                    backingNode.put(propName, new JsonValue(arg));
                } else if (Proxy.isProxyClass(arg.getClass()) && Proxy.getInvocationHandler(arg) instanceof JsonInvocationHandler ih) {
                     backingNode.put(propName, ih.backingNode);
                }
                return null;
            } else if (methodName.equals("toString")) {
                return backingNode.toString();
            }

            throw new UnsupportedOperationException("JsonProxy only supports getters and setters.");
        }

        private String getPropertyName(Method method) {
            if (method.isAnnotationPresent(JsonProperty.class)) {
                String val = method.getAnnotation(JsonProperty.class).value();
                if (!val.isEmpty()) return val;
            }
            String name = method.getName();
            if (name.startsWith("get") || name.startsWith("set")) {
                name = name.substring(3);
            } else if (name.startsWith("is")) {
                name = name.substring(2);
            }
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }

        private Object castValue(Object value, Class<?> returnType) {
            if (value == null) return null;
            if (returnType == String.class) return value.toString();
            if (returnType == int.class || returnType == Integer.class) return ((Number) value).intValue();
            if (returnType == double.class || returnType == Double.class) return ((Number) value).doubleValue();
            if (returnType == boolean.class || returnType == Boolean.class) return (Boolean) value;
            return value;
        }
    }
}
