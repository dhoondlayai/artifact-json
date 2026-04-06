package io.github.dhoondlayai.artifact.json.convert;

import io.github.dhoondlayai.artifact.json.model.JsonArray;
import io.github.dhoondlayai.artifact.json.model.JsonObject;
import io.github.dhoondlayai.artifact.json.streaming.FastJsonEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonConverter (Multi-Format Suite) Tests")
public class JsonConverterTest {

    private JsonObject obj;
    private JsonArray books;

    @BeforeEach
    void setup() {
        obj = new JsonObject().put("name", "Artifact").put("version", 2.0);
        books = new JsonArray()
                .add(new JsonObject().put("title", "1984").put("price", 10.0))
                .add(new JsonObject().put("title", "Dune").put("price", 20.0));
    }

    @Test
    @DisplayName("PRETTY JSON conversion")
    void testToPrettyString() {
        String pretty = JsonConverter.toPrettyString(obj, 2);
        assertTrue(pretty.contains("  \"name\": \"Artifact\""));
    }

    @Test
    @DisplayName("CSV round-trip conversion")
    void testCsvRoundTrip() {
        String csv = JsonConverter.toCsv(books);
        assertTrue(csv.contains("title,price"));
        
        JsonArray backParsed = JsonConverter.fromCsv(csv);
        assertEquals(2, backParsed.size());
        assertEquals("1984", ((JsonObject) backParsed.get(0)).get("title").asText());
    }

    @Test
    @DisplayName("XML conversion")
    void testToXml() {
        String xml = JsonConverter.toXml(obj, "root");
        assertTrue(xml.contains("<root>"));
        assertTrue(xml.contains("<name>Artifact</name>"));
    }

    @Test
    @DisplayName("YAML round-trip conversion")
    void testYamlRoundTrip() {
        String yaml = JsonConverter.toYaml(obj);
        assertTrue(yaml.contains("name: Artifact"));
        
        JsonObject backParsed = (JsonObject) JsonConverter.fromYaml(yaml);
        assertEquals("Artifact", backParsed.getString("name").orElse(null));
    }

    @Test
    @DisplayName("Properties conversion")
    void testToProperties() {
        String props = JsonConverter.toProperties(obj);
        assertTrue(props.contains("name=Artifact"));
        assertTrue(props.contains("version=2.0"));
    }

    @Test
    @DisplayName("Markdown Table conversion")
    void testToMarkdownTable() {
        String md = JsonConverter.toMarkdownTable(books);
        assertTrue(md.contains("| title | price |"));
        assertTrue(md.contains("| 1984 | 10.0 |"));
    }

    @Test
    @DisplayName("HTML Table conversion")
    void testToHtmlTable() {
        String html = JsonConverter.toHtmlTable(books, "data-table");
        assertTrue(html.contains("class=\"data-table\""));
        assertTrue(html.contains("<td>1984</td>"));
    }
}
