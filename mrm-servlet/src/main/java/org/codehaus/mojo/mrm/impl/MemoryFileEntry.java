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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.codehaus.mojo.mrm.api.BaseFileEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.FileSystem;

/**
 * A {@link org.codehaus.mojo.mrm.api.FileEntry} who's contents are held in memory.
 */
public class MemoryFileEntry extends BaseFileEntry {

    /**
     * Ensure consistent serialization.
     *
     * @since 1.0
     */
    private static final long serialVersionUID = 1L;

    /**
     * Lazy <code>null</code> surrogate.
     *
     * @since 1.0
     */
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * The content to serve.
     *
     * @since 1.0
     */
    private final byte[] content;

    /**
     * The timestamp of the content.
     *
     * @since 1.0
     */
    private final long lastModified;

    /**
     * Creates a new entry for the specified parent directory of the specified file system with the specified name
     * having the supplied content.
     *
     * @param fileSystem the file system.
     * @param parent     the parent directory
     * @param name       the name.
     * @param content    the content.
     * @since 1.0
     */
    public MemoryFileEntry(FileSystem fileSystem, DirectoryEntry parent, String name, byte[] content) {
        super(fileSystem, parent, name);
        this.content = content == null ? EMPTY_BYTE_ARRAY : content;
        this.lastModified = System.currentTimeMillis();
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() {
        return content.length;
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }
}
