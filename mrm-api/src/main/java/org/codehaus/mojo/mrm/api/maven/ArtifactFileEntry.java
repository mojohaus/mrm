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

package org.codehaus.mojo.mrm.api.maven;

import org.codehaus.mojo.mrm.api.BaseFileEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.FileSystem;

import java.io.IOException;
import java.io.InputStream;

public class ArtifactFileEntry
    extends BaseFileEntry
{

    private final Artifact artifact;

    private final ArtifactStore store;

    protected ArtifactFileEntry( FileSystem fileSystem, DirectoryEntry parent, Artifact artifact, ArtifactStore store )
    {
        super( fileSystem, parent, artifact.getName() );
        this.artifact = artifact;
        this.store = store;
    }

    public long getSize()
        throws IOException
    {
        try
        {
            return store.getSize( artifact );
        }
        catch ( ArtifactNotFoundException e )
        {
            IOException ioe = new IOException( "Artifact does not exist" );
            ioe.initCause( e );
            throw ioe;
        }
    }

    public InputStream getInputStream()
        throws IOException
    {
        try
        {
            return store.get( artifact );
        }
        catch ( ArtifactNotFoundException e )
        {
            IOException ioe = new IOException( "Artifact does not exist" );
            ioe.initCause( e );
            throw ioe;
        }
    }

    public long getLastModified()
        throws IOException
    {
        try
        {
            return store.getLastModified( artifact );
        }
        catch ( ArtifactNotFoundException e )
        {
            IOException ioe = new IOException( "Artifact does not exist" );
            ioe.initCause( e );
            throw ioe;
        }
    }

    public Artifact getArtifact()
    {
        return artifact;
    }
}
