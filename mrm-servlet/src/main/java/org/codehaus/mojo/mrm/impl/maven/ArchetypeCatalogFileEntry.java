/*
 * Copyright 2013 Robert Scholte
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

package org.codehaus.mojo.mrm.impl.maven;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Writer;
import org.codehaus.mojo.mrm.api.BaseFileEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.FileSystem;
import org.codehaus.mojo.mrm.api.maven.ArchetypeCatalogNotFoundException;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;

/**
 * A file entry backed by {@link ArchetypeCatalog} in a {@link ArtifactStore}.
 *
 * @since 1.0
 */
public class ArchetypeCatalogFileEntry extends BaseFileEntry {

    /**
     * The backing {@link ArtifactStore}.
     *
     * @since 1.0
     */
    private final ArtifactStore store;

    /**
     * Creates a file entry for the specified parent directory of the specified file system that corresponds to the
     * specified metadata in the specified artifact store and will have the name
     * <code>maven-metadata.xml</code>.
     *
     * @param fileSystem the file system.
     * @param parent     the parent directory.
     * @param store      the artifact store.
     * @since 1.0
     */
    public ArchetypeCatalogFileEntry(FileSystem fileSystem, DirectoryEntry parent, ArtifactStore store) {
        super(fileSystem, parent, "archetype-catalog.xml");
        this.store = store;
    }

    @Override
    public long getSize() throws IOException {
        try {
            ArchetypeCatalog metadata = store.getArchetypeCatalog();
            ArchetypeCatalogXpp3Writer writer = new ArchetypeCatalogXpp3Writer();
            StringWriter stringWriter = new StringWriter();
            writer.write(stringWriter, metadata);
            return stringWriter.toString().getBytes().length;
        } catch (ArchetypeCatalogNotFoundException e) {
            throw new IOException("File not found", e);
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            ArchetypeCatalog metadata = store.getArchetypeCatalog();
            ArchetypeCatalogXpp3Writer writer = new ArchetypeCatalogXpp3Writer();
            StringWriter stringWriter = new StringWriter();
            writer.write(stringWriter, metadata);
            return new ByteArrayInputStream(stringWriter.toString().getBytes());
        } catch (ArchetypeCatalogNotFoundException e) {
            return null;
        }
    }

    @Override
    public long getLastModified() throws IOException {
        try {
            return store.getArchetypeCatalogLastModified();
        } catch (ArchetypeCatalogNotFoundException e) {
            throw new IOException("File not found", e);
        }
    }
}
