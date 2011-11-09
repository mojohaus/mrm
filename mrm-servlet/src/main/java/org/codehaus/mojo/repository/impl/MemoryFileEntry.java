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

package org.codehaus.mojo.repository.impl;

import org.codehaus.mojo.repository.api.BaseFileEntry;
import org.codehaus.mojo.repository.api.DirectoryEntry;
import org.codehaus.mojo.repository.api.FileSystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MemoryFileEntry
    extends BaseFileEntry
{

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private final byte[] content;

    private final long lastModified;

    public MemoryFileEntry( FileSystem fileSystem, DirectoryEntry parent, String name, byte[] content )
    {
        super( fileSystem, parent, name );
        this.content = content == null ? EMPTY_BYTE_ARRAY : content;
        this.lastModified = System.currentTimeMillis();
    }

    public long getLastModified()
    {
        return lastModified;
    }

    public long getSize()
    {
        return content.length;
    }

    public InputStream getInputStream()
        throws IOException
    {
        return new ByteArrayInputStream( content );
    }
}
