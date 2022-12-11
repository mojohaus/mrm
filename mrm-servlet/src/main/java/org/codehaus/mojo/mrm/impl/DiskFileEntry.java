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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.codehaus.mojo.mrm.api.BaseFileEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.FileSystem;

/**
 * A file entry backed by a {@link File} on a local disk.
 *
 * @since 1.0
 */
public class DiskFileEntry extends BaseFileEntry {

    /**
     * Ensure consistent serialization.
     *
     * @since 1.0
     */
    private static final long serialVersionUID = 1L;

    /**
     * The backing file.
     *
     * @since 1.0
     */
    private final File file;

    /**
     * Creates a new instance in the specified parent directory of the specified file system that backs the supplied
     * file named with {@link java.io.File#getName()}.
     *
     * @param fileSystem the file system.
     * @param parent     the parent directory.
     * @param file       the backing file.
     * @since 1.0
     */
    public DiskFileEntry(FileSystem fileSystem, DirectoryEntry parent, File file) {
        this(fileSystem, parent, file.getName(), file);
    }

    /**
     * Creates a new instance in the specified parent directory of the specified file system that backs the supplied
     * file named with the supplied name.
     *
     * @param fileSystem the file system.
     * @param parent     the parent directory.
     * @param name       the name of the entry.
     * @param file       the backing file.
     * @since 1.0
     */
    public DiskFileEntry(FileSystem fileSystem, DirectoryEntry parent, String name, File file) {
        super(fileSystem, parent, name);
        this.file = file;
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified() {
        return file.lastModified();
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() {
        return file.length();
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }
}
