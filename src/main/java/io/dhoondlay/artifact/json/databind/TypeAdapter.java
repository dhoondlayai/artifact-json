package io.dhoondlay.artifact.json.databind;

import io.dhoondlay.artifact.json.model.*;
import java.util.*;

/**
 * TypeAdapter: Custom logic for complex type mappings.
 */
public interface TypeAdapter<T> {
    JsonNode toJson(T value) throws Exception;
    T fromJson(JsonNode node) throws Exception;
}
