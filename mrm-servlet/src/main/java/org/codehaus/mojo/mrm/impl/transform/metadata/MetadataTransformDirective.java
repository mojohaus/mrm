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
package org.codehaus.mojo.mrm.impl.transform.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.UnaryOperator;

import org.codehaus.mojo.mrm.impl.transform.FileTransformPlan;
import org.codehaus.mojo.mrm.impl.transform.TransformDirectiveSource;

/**
 * <p>
 * Provides a plan based on {@code ".mrm-transform.properties"}.
 * </p>
 *
 * Supports:
 * <dl>
 *   <dt>[inputName].target=[output]</dt>
 *   <dd>Transform the filename  from input to output</dd>
 * </dl>
 *
 * @since 2.0.0
 */
public final class MetadataTransformDirective implements TransformDirectiveSource {

    private final Path metadataFile = Path.of(".mrm-transform.properties");

    private final Properties properties;

    /**
     *
     * @param root the archive directory
     */
    public MetadataTransformDirective(Path root) {
        this.properties = new Properties();

        try (InputStream in = Files.newInputStream(root.resolve(metadataFile))) {
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public FileTransformPlan plan() {
        return new MetadataTransformPlan(key -> properties.getProperty(key + ".targetName", key));
    }

    private static class MetadataTransformPlan implements FileTransformPlan {

        private final UnaryOperator<String> filenameTransformer;

        MetadataTransformPlan(UnaryOperator<String> filenameTransformer) {
            this.filenameTransformer = filenameTransformer;
        }

        @Override
        public UnaryOperator<String> filename() {
            return filenameTransformer;
        }
    }
}
