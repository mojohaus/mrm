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

/**
 * Base implementation of {@link FileEntry} that all implementations should extend from.
 *
 * @since 1.0
 */
public abstract class BaseFileEntry extends AbstractEntry implements FileEntry {

    /**
     * Creates an entry in the specified file system with the specified parent and name.
     *
     * @param fileSystem The filesystem.
     * @param parent     The parent.
     * @param name       The name of the entry.
     * @since 1.0
     */
    protected BaseFileEntry(FileSystem fileSystem, DirectoryEntry parent, String name) {
        super(fileSystem, parent, name);
    }
}
