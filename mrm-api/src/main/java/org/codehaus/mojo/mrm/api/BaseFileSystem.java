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
 * Base implementation of {@link FileSystem} that all implementations should extend from.
 *
 * @since 1.0
 */
public abstract class BaseFileSystem implements FileSystem {

    /**
     * The root entry.
     */
    private final DirectoryEntry root = new DefaultDirectoryEntry(this, null, "");

    @Override
    public DirectoryEntry getRoot() {
        return root;
    }

    @Override
    public Entry get(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.length() == 0) {
            return root;
        }
        String[] parts = path.split("/");
        if (parts.length == 0) {
            return root;
        }
        DirectoryEntry parent = root;
        for (int i = 0; i < parts.length - 1; i++) {
            parent = new DefaultDirectoryEntry(this, parent, parts[i]);
        }
        return get(parent, parts[parts.length - 1]);
    }

    /**
     * Gets the named entry in the specified directory.
     * The default implementation lists all the entries in the directory and looks for the one with the matching name.
     * Caching implementations will likely want to override this behaviour.
     *
     * @param parent the directory.
     * @param name   the name of the entry to get.
     * @return the {@link Entry} or <code>null</code> if the entry does not exist.
     */
    protected Entry get(DirectoryEntry parent, String name) {
        parent.getClass();
        Entry[] entries = listEntries(parent);
        if (entries != null) {
            for (Entry entry : entries) {
                if (name.equals(entry.getName())) {
                    return entry;
                }
            }
        }
        return null;
    }
}
