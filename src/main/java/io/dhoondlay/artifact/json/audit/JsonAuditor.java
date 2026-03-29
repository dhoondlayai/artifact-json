package io.dhoondlay.artifact.json.audit;

import io.dhoondlay.artifact.json.model.*;
import java.util.*;
import java.time.LocalDateTime;

/**
 * 🕵️ JSON Audit Trail: Tracks and logs every modification to the JSON tree.
 * Essential for banking, healthcare, or security-sensitive applications.
 */
public class JsonAuditor {
    private final List<AuditLog> history = new ArrayList<>();

    public record AuditLog(LocalDateTime timestamp, String action, String path, Object oldValue, Object newValue) {
    }

    /**
     * Watches a JsonObject for changes.
     */
    public void trackChange(String action, String path, Object oldVal, Object newVal) {
        history.add(new AuditLog(LocalDateTime.now(), action, path, oldVal, newVal));
    }

    public List<AuditLog> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /**
     * Prints a beautiful audit report.
     */
    public void printReport() {
        System.out.println("\n📜 --- JSON MODIFICATION AUDIT REPORT ---");
        history.forEach(log -> System.out.printf("[%s] %s at %s: %s -> %s\n",
                log.timestamp, log.action, log.path, log.oldValue, log.newValue));
    }
}
