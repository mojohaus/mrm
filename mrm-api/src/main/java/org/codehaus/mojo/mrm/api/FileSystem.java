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
import java.io.InputStream;

/**
 * A repository is just a type of file system.
 *
 * @since 1.0
 */
public interface FileSystem {
    /**
     * Lists the entries in the specified directory. Some implementations may be lazy caching
     * implementations, in which case it is permitted to return either an empty array, or only those entries which
     * have been loaded into the cache, so consumers should not assume that a missing entry implies non-existence.
     * The only way to be sure that an artifact does not exist is to call the {@link FileEntry#getInputStream()} method,
     * although {@link #get(String)} returning <code>null</code> is also definitive, a non-null does not prove
     * existence).
     *
     * @param directory the directory to list the entries of.
     * @return a copy of the known entries in the specified directory, never <code>null</code>. The caller can safely
     *         modify the returned array.
     * @since 1.0
     */
    Entry[] listEntries(DirectoryEntry directory);

    /**
     * Returns the root directory entry.
     *
     * @return the root directory entry.
     * @since 1.0
     */
    DirectoryEntry getRoot();

    /**
     * Returns the entry at the specified path. A <code>null</code> result proves the path does not exist, however
     * very lazy caching implementations may return a non-null entry for paths which do not exist.
     *
     * @param path the path to retrieve the {@link Entry} of.
     * @return the {@link Entry} or <code>null</code> if the path definitely does not exist.
     * @since 1.0
     */
    Entry get(String path);

    /**
     * Returns the time that the specified directory entry was last modified. Note:
     * {@link DefaultDirectoryEntry#getLastModified()} delegates to this method.
     *
     * @param entry the directory entry.
     * @return A <code>long</code> value representing the time the directory was
     *         last modified, measured in milliseconds since the epoch
     *         (00:00:00 GMT, January 1, 1970), or <code>0L</code> if the
     *         the time is unknown.
     * @throws IOException if an I/O error occurs.
     * @since 1.0
     */
    long getLastModified(DirectoryEntry entry) throws IOException;

    /**
     * Puts the specified content into a the specified directory.
     *
     * @param parent  the directory in which the content is to be created/updated.
     * @param name    the name of the file.
     * @param content the content (implementer is responsible for closing).
     * @return the {@link FileEntry} that was created/updated.
     * @throws UnsupportedOperationException if the repository is read-only.
     * @throws java.io.IOException           if the content could not be read/written.
     * @since 1.0
     */
    FileEntry put(DirectoryEntry parent, String name, InputStream content) throws IOException;
}
