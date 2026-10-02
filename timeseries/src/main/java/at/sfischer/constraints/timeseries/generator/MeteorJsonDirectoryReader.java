package at.sfischer.constraints.timeseries.generator;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.generator.InputGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class MeteorJsonDirectoryReader implements InputGenerator {

    private final String model;
    private final List<Path> jsonFiles;

    private int currentFileIndex = 0;
    private MeteorJsonReader currentReader;

    public MeteorJsonDirectoryReader(
            String directory,
            String model
    ) {
        this.model = model;
        try (Stream<Path> paths = Files.walk(Path.of(directory))) {
            this.jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .sorted()
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON directory: " + directory, e);
        }
    }

    @Override
    public String getIdentifier() {
        return "MeteorJsonDirectoryReader";
    }

    @Override
    public Stream<DataObject> generate(SimpleDataSchema schema) {
        return Stream.generate(() -> generateInput(schema))
                .takeWhile(Objects::nonNull);
    }

    private DataObject generateInput(SimpleDataSchema schema) {
        while (true) {
            // No files left
            if (currentFileIndex >= jsonFiles.size()) {
                return null;
            }

            // Load the next file if necessary
            if (currentReader == null) {
                Path currentFile = jsonFiles.get(currentFileIndex);
                currentReader = new MeteorJsonReader(currentFile.toString(), model);
            }

            // Ask the current file for its next window
            DataObject result = currentReader.generate(schema).findFirst().orElse(null);
            if (result != null) {
                return result;
            }

            // Current file is exhausted
            currentReader = null;
            currentFileIndex++;
        }
    }
}