/*
 * Copyright 2026 Robert Scholte
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.mojo.mrm.jupiter;

import java.nio.file.Path;

/**
 * Utility class that resolves the directory and prevents path traversal
 *
 * @since 2.0.0
 */
final class DirectoryResolver {

    static Path resolve(Directory directory) {
        PathResolver basePathResolver = getBasePathResolver(directory);

        Path basePath = basePathResolver.resolve().toAbsolutePath();

        Path targetPath = Path.of(directory.value());
        if (targetPath.isAbsolute()) {
            throw new IllegalArgumentException("path of @CacheDirectory must be relative");
        }

        Path targetDirectory = basePath.resolve(targetPath);
        if (!targetDirectory.startsWith(basePath)) {
            throw new SecurityException("Path traversal not allowed");
        }

        return targetDirectory;
    }

    private static PathResolver getBasePathResolver(Directory directory) {
        try {
            return directory.baseBathResolver().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(directory.baseBathResolver() + " is missing default constructor");
        }
    }
}
