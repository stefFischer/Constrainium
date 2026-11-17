package at.scch.traceon;

import at.scch.traceon.otel.graph.GraphJsonFiles;
import at.sfischer.constraints.data.model.Graph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GraphJsonFilesTest {
    @Test
    void replacesWhitespaceAndInvalidRunsWhileRetainingReadableNames() {
        assertEquals("GET_products_{productId}", GraphJsonFiles.sanitizeFileName("GET /products/{productId}"));
        assertEquals("a_b_c", GraphJsonFiles.sanitizeFileName("a \t/:*?\"<>|\\b\n c"));
        assertEquals("Grün", GraphJsonFiles.sanitizeFileName("Grün"));
        assertEquals("graph", GraphJsonFiles.sanitizeFileName(""));
        assertEquals("graph", GraphJsonFiles.sanitizeFileName(null));
    }

    @Test
    void handlesWindowsDeviceNamesTrailingDotsAndLongNames() {
        assertEquals("_CON", GraphJsonFiles.sanitizeFileName("CON"));
        assertEquals("_nul.txt", GraphJsonFiles.sanitizeFileName("nul.txt"));
        assertEquals("_", GraphJsonFiles.sanitizeFileName(".."));
        assertEquals("name_", GraphJsonFiles.sanitizeFileName("name..."));
        assertTrue(GraphJsonFiles.sanitizeFileName("a".repeat(300)).length() <= 200);
    }

    @Test
    void avoidsExistingFilesAndSanitizationCollisions(@TempDir Path directory) throws Exception {
        Path original = directory.resolve("GET_products.json");
        Files.writeString(original, "existing content");
        Path first = GraphJsonFiles.writeUnique(new Graph(), directory, "GET /products");
        Path second = GraphJsonFiles.writeUnique(new Graph(), directory, "GET:products");
        assertEquals("GET_products_2.json", first.getFileName().toString());
        assertEquals("GET_products_3.json", second.getFileName().toString());
        assertEquals("existing content", Files.readString(original));
        assertTrue(Files.readString(first).contains("constrainium-call-graph"));
        assertEquals(directory, first.getParent());
    }
}
