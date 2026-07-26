package org.codehaus.mojo.mrm.jupiter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Common operations on directories
 *
 * @since 2.0.0
 */
final class FileUtils {

    private FileUtils() {}

    static void cleanDirectory(Path rootDir) throws IOException {
        if (!Files.exists(rootDir)) {
            throw new IllegalArgumentException("Directory does not exist: " + rootDir);
        }

        try (Stream<Path> stream = Files.walk(rootDir)) {
            stream.sorted(Comparator.reverseOrder()) // Deletes children files before parent folders
                    .filter(path -> !path.equals(rootDir)) // Prevents deleting the target root folder itself
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException("Failed to delete: " + path, e);
                        }
                    });
        }
    }

    static void copyDirectory(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Source directory does not exist: " + source);
        }

        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(sourcePath -> {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                try {
                    if (Files.isDirectory(sourcePath)) {
                        if (!Files.exists(targetPath)) {
                            Files.createDirectories(targetPath);
                        }
                    } else {
                        Files.copy(
                                sourcePath,
                                targetPath,
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.COPY_ATTRIBUTES);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to copy from " + sourcePath + " to " + targetPath, e);
                }
            });
        }
    }
}
