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

package org.codehaus.mojo.mrm.impl.digest;

import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.FileEntry;
import org.codehaus.mojo.mrm.api.FileSystem;

/**
 * A factory for creating digest file entries.
 *
 * @since 1.0
 */
public interface DigestFileEntryFactory
{
    /**
     * Returns the type of digest (i.e. the file extension).
     *
     * @return the type of digest (i.e. the file extension).
     * @since 1.0
     */
    String getType();

    /**
     * Creates a digest entry for the specified content within the specified directory of the file system.
     *
     * @param fileSystem the file system the digest entry will be created in.
     * @param parent     the parent directory that the digest entry will belong to.
     * @param entry      the entry that the digest entry will digest.
     * @return a digest file entry.
     * @since 1.0
     */
    FileEntry create( FileSystem fileSystem, DirectoryEntry parent, FileEntry entry );

}
