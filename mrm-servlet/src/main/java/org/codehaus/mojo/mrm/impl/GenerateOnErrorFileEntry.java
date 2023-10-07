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
 * A delegating file entry that also knows how to generate the content if the entry it delegates to has problems.
 *
 * @since 1.0
 */
public class GenerateOnErrorFileEntry extends BaseFileEntry {

    /**
     * The entry that we can fall back to if there is an {@link #error}.
     *
     * @since 1.0
     */
    private final FileEntry generatorEntry;

    /**
     * The entry that we delegate to (unless there is an {@link #error}).
     *
     * @since 1.0
     */
    private final FileEntry delegateEntry;

    /**
     * Flag to indicate if we have hit an error accessing the {@link #delegateEntry} (<code>true</code>) or whether
     * the {@link #delegateEntry} should still be tried.
     *
     * @since 1.0
     */
    private transient volatile boolean error;

    /**
     * Creates a {@link FileEntry} in the specified directory of the specified file system that delegates to another
     * {@link FileEntry} but can (and will) fall back to another {@link FileEntry} if the primary delegate has an
     * error.
     *
     * @param fileSystem     The file system.
     * @param parent         The parent directory.
     * @param delegateEntry  The primary delegate entry.
     * @param generatorEntry The delegate entry that is more costly to use and therefore should only be used if the
     *                       primary delegate has an error.
     * @since 1.0
     */
    public GenerateOnErrorFileEntry(
            FileSystem fileSystem, DirectoryEntry parent, FileEntry delegateEntry, FileEntry generatorEntry) {
        super(fileSystem, parent, delegateEntry.getName());
        this.generatorEntry = generatorEntry;
        this.delegateEntry = delegateEntry;
    }

    @Override
    public long getLastModified() throws IOException {
        if (!error) {
            try {
                return delegateEntry.getLastModified();
            } catch (IOException e) {
                error = true;
                return generatorEntry.getLastModified();
            }
        } else {
            return generatorEntry.getLastModified();
        }
    }

    @Override
    public long getSize() throws IOException {
        if (!error) {
            try {
                return delegateEntry.getSize();
            } catch (IOException e) {
                error = true;
                return generatorEntry.getSize();
            }
        } else {
            return generatorEntry.getSize();
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (!error) {
            try {
                return delegateEntry.getInputStream();
            } catch (IOException e) {
                error = true;
                return generatorEntry.getInputStream();
            }
        } else {
            return generatorEntry.getInputStream();
        }
    }
}
