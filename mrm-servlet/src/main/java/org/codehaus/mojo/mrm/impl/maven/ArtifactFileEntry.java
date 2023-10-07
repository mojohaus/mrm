/*
 * Copyright 2011 Stephen Connolly
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

import java.io.IOException;
import java.io.InputStream;

import org.codehaus.mojo.mrm.api.BaseFileEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.FileSystem;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;

/**
 * A file entry backed by a {@link Artifact} in a {@link ArtifactStore}.
 *
 * @since 1.0
 */
public class ArtifactFileEntry extends BaseFileEntry {

    /**
     * The backing {@link Artifact}.
     *
     * @since 1.0
     */
    private final Artifact artifact;

    /**
     * The backing {@link ArtifactStore}.
     *
     * @since 1.0
     */
    private final ArtifactStore store;

    /**
     * Creates a file entry for the specified parent directory of the specified file system that corresponds to the
     * specified artifact in the specified artifact store and will have the name
     * {@link org.codehaus.mojo.mrm.api.maven.Artifact#getName()}.
     *
     * @param fileSystem the file system.
     * @param parent     the parent directory.
     * @param artifact   the artifact.
     * @param store      the artifact store.
     * @since 1.0
     */
    protected ArtifactFileEntry(FileSystem fileSystem, DirectoryEntry parent, Artifact artifact, ArtifactStore store) {
        super(fileSystem, parent, artifact.getName());
        this.artifact = artifact;
        this.store = store;
    }

    @Override
    public long getSize() throws IOException {
        try {
            return store.getSize(artifact);
        } catch (ArtifactNotFoundException e) {
            throw new IOException("Artifact does not exist", e);
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return store.get(artifact);
        } catch (ArtifactNotFoundException e) {
            throw new IOException("Artifact does not exist", e);
        }
    }

    @Override
    public long getLastModified() throws IOException {
        try {
            return store.getLastModified(artifact);
        } catch (ArtifactNotFoundException e) {
            throw new IOException("Artifact does not exist", e);
        }
    }
}
