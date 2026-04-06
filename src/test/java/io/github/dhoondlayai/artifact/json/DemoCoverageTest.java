package io.github.dhoondlayai.artifact.json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("Demo Execution Coverage Test")
public class DemoCoverageTest {

    @Test
    @DisplayName("Executes ArtifactJsonProjectDemo to hit edge cases and demos")
    void testDemoExecution() {
        // Redirect stdout to avoid cluttering test logs
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        
        try {
            assertDoesNotThrow(() -> ArtifactJsonProjectDemo.main(new String[]{}));
        } finally {
            System.setOut(originalOut);
        }
    }
}
