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

package org.codehaus.mojo.mrm.api;

import java.io.IOException;

/**
 * Default implementation of {@link DirectoryEntry}.
 *
 * @since 1.0
 */
public class DefaultDirectoryEntry extends AbstractEntry implements DirectoryEntry {
    /**
     * Ensure consistent serialization.
     *
     * @since 1.0
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an entry in the specified file system with the specified parent and name.
     *
     * @param fileSystem The filesystem.
     * @param parent     The parent (or <code>null</code> if this is the root entry).
     * @param name       The name of the entry (or the empty string if this is the root entry).
     * @since 1.0
     */
    public DefaultDirectoryEntry(FileSystem fileSystem, DirectoryEntry parent, String name) {
        super(fileSystem, parent, name);
    }

    /**
     * Creates a {@link DefaultDirectoryEntry} that is equivalent to the supplied {@link DirectoryEntry} only in
     * the specified target {@link FileSystem}.
     *
     * @param target    the filesystem.
     * @param directory the directory.
     * @return a {@link DirectoryEntry} in the target filesystem.
     * @since 1.0
     */
    public static DirectoryEntry equivalent(FileSystem target, DirectoryEntry directory) {
        if (target.equals(directory.getFileSystem())) {
            return directory;
        }
        if (directory.getParent() == null) {
            return target.getRoot();
        }
        return new DefaultDirectoryEntry(target, equivalent(target, directory.getParent()), directory.getName());
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified() throws IOException {
        return getFileSystem().getLastModified(this);
    }
}
