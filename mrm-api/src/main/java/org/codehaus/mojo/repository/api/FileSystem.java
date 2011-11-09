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

package org.codehaus.mojo.repository.api;

import java.io.IOException;
import java.io.InputStream;

public interface FileSystem
{
    Entry[] listEntries( DirectoryEntry directory );

    DirectoryEntry getRoot();

    Entry get( String path );

    long getLastModified( DirectoryEntry entry )
        throws IOException;

    DirectoryEntry mkdir( DirectoryEntry parent, String name );

    FileEntry put( DirectoryEntry parent, String name, InputStream content )
        throws IOException;

    FileEntry put( DirectoryEntry parent, String name, byte[] content )
        throws IOException;

    void remove( Entry entry );
}
