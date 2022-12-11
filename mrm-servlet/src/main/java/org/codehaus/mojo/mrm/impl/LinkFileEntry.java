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

package org.codehaus.mojo.mrm.impl;

import java.io.IOException;
import java.io.InputStream;

import org.codehaus.mojo.mrm.api.BaseFileEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.FileEntry;
import org.codehaus.mojo.mrm.api.FileSystem;

/**
 * An entry backed by a {@link FileEntry} on a (possibly different) {@link FileSystem}.
 *
 * @since 1.0
 */
public class LinkFileEntry extends BaseFileEntry {

    /**
     * Ensure consistent serialization.
     *
     * @since 1.0
     */
    private static final long serialVersionUID = 1L;

    /**
     * The backing entry.
     *
     * @since 1.0
     */
    private final FileEntry entry;

    /**
     * Creates a new instance in the specified parent directory of the specified file system that backs the supplied
     * entry named with {@link FileEntry#getName()}.
     *
     * @param fileSystem the file system.
     * @param parent     the parent directory.
     * @param entry      the backing entry.
     * @since 1.0
     */
    public LinkFileEntry(FileSystem fileSystem, DirectoryEntry parent, FileEntry entry) {
        this(fileSystem, parent, entry.getName(), entry);
    }

    /**
     * Creates a new instance in the specified parent directory of the specified file system that backs the supplied
     * entry named with the supplied name.
     *
     * @param fileSystem the file system.
     * @param parent     the parent directory.
     * @param name       the name of the entry.
     * @param entry      the backing entry.
     * @since 1.0
     */
    public LinkFileEntry(FileSystem fileSystem, DirectoryEntry parent, String name, FileEntry entry) {
        super(fileSystem, parent, name);
        this.entry = entry;
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified() throws IOException {
        return entry.getLastModified();
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() throws IOException {
        return entry.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getInputStream() throws IOException {
        return entry.getInputStream();
    }
}
