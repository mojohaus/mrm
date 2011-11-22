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

import org.codehaus.mojo.mrm.api.BaseFileEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.FileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A {@link org.codehaus.mojo.mrm.api.FileEntry} that is hosted at a remote {@link URL}.
 */
public class RemoteFileEntry
    extends BaseFileEntry
{

    /**
     * Ensure consistent serialization.
     *
     * @since 1.0
     */
    private static final long serialVersionUID = 1L;

    /**
     * The {@link URL} of the entry.
     */
    private final URL url;

    /**
     * Create a new file entry.
     *
     * @param fileSystem the owning file system.
     * @param parent     the directory hosting the entry.
     * @param name       the name of the entry.
     * @param url        the content of the entry.
     */
    public RemoteFileEntry( FileSystem fileSystem, DirectoryEntry parent, String name, URL url )
    {
        super( fileSystem, parent, name );
        this.url = url;
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified()
        throws IOException
    {
        return url.openConnection().getLastModified();
    }

    /**
     * {@inheritDoc}
     */
    public long getSize()
        throws IOException
    {
        return url.openConnection().getContentLength();
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getInputStream()
        throws IOException
    {
        return url.openConnection().getInputStream();
    }
}
