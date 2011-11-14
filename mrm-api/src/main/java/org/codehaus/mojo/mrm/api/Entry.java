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
import java.io.Serializable;

/**
 * An entry in the repository.
 *
 * @since 1.0
 */
public interface Entry
    extends Serializable
{

    /**
     * Returns the repository that this entry belongs to.
     *
     * @return the repository that this entry belongs to.
     * @since 1.0
     */
    FileSystem getFileSystem();

    /**
     * Returns the parent of this entry (or <code>null</code> if there is no parent, that is the root entry).
     *
     * @return the parent of this entry (or <code>null</code> if there is no parent, that is the root entry).
     * @since 1.0
     */
    DirectoryEntry getParent();

    /**
     * Returns the name of this entry.
     *
     * @return the name of this entry.
     * @since 1.0
     */
    String getName();

    /**
     * Returns the time that this entry was last modified. Note that {@link DirectoryEntry}s may
     * delegate to {@link FileSystem#getLastModified(DirectoryEntry)}.
     *
     * @return A <code>long</code> value representing the time the directory was
     *         last modified, measured in milliseconds since the epoch
     *         (00:00:00 GMT, January 1, 1970), or <code>0L</code> if the
     *         the time is unknown.
     * @throws IOException if an I/O error occurs.
     * @since 1.0
     */
    long getLastModified()
        throws IOException;

    /**
     * Returns the path of this entry relative to the root of the filesystem.
     *
     * @return the path of this entry relative to the root of the filesystem.
     * @since 1.0
     */
    String toPath();
}
