package io.dhoondlay.artifact.json.streaming;

import io.dhoondlay.artifact.json.model.JsonNode;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * Streaming Layer: Event-driven parsing and generation.
 */
public interface StreamingProcessor {
    /**
     * Non-blocking parsing logic using java.nio for high-performance processing.
     */
    JsonNode parse(ReadableByteChannel input) throws IOException;

    /**
     * Non-blocking generation targeting direct off-heap or ByteBuffer outputs.
     */
    void generate(JsonNode node, ByteBuffer output) throws IOException;
}
