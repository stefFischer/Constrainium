package at.scch.traceon.otel.graph;

import at.sfischer.constraints.data.model.Graph;
import at.sfischer.constraints.data.model.utils.GraphJsonExporter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Safe filenames for graph exports, including repeated names and repeated runs. */
public final class GraphJsonFiles {
    private GraphJsonFiles() {}

    /** Sanitizes a base name, without adding the .json extension. */
    public static String sanitizeFileName(String name) {
        String safe = name == null ? "" : name.replaceAll("[\\s\\p{Z}\\p{Cc}<>:\"/\\\\|?*]+", "_");
        // Leave room for the extension and collision suffix within a filename component.
        if (safe.length() > 200) {
            int end = Character.isHighSurrogate(safe.charAt(199)) ? 199 : 200;
            safe = safe.substring(0, end);
        }
        safe = safe.replaceAll("\\.+$", "_").replaceAll("_+", "_");
        if (safe.isEmpty()) safe = "graph";
        // Windows device names are reserved even when followed by an extension.
        if (safe.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9¹²³]|LPT[1-9¹²³])(?:\\..*)?")) safe = "_" + safe;
        return safe;
    }

    /** Creates name.json, name_2.json, etc., without overwriting an existing file. */
    public static Path writeUnique(Graph graph, Path directory, String name) throws IOException {
        String base = sanitizeFileName(name);
        String json = GraphJsonExporter.export(graph);
        for (long index = 1; ; index++) {
            Path candidate = directory.resolve(base + (index == 1 ? "" : "_" + index) + ".json");
            try {
                // CREATE_NEW makes collision handling atomic, including concurrent writers.
                Files.writeString(candidate, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                return candidate;
            } catch (FileAlreadyExistsException ignored) {
                // Includes files from previous runs and names that sanitize identically.
            }
        }
    }
}
