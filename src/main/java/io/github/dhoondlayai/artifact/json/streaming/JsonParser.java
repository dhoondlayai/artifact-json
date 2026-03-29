package io.github.dhoondlayai.artifact.json.streaming;

import java.io.IOException;

/**
 * JsonParser: The "Engine" for tokenization.
 */
public interface JsonParser {
    enum Token {
        START_OBJECT, END_OBJECT,
        START_ARRAY, END_ARRAY,
        FIELD_NAME, VALUE_STRING, VALUE_NUMBER,
        VALUE_BOOLEAN, VALUE_NULL
    }

    Token nextToken() throws IOException;
    String getCurrentName() throws IOException;
    String getText() throws IOException;
    Number getNumberValue() throws IOException;
}
