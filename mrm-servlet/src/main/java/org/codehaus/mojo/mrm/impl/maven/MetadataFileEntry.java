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

package org.codehaus.mojo.mrm.impl.maven;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.mojo.mrm.api.BaseFileEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.FileSystem;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * A file entry backed by {@link Metadata} in a {@link ArtifactStore}.
 *
 * @since 1.0
 */
public class MetadataFileEntry
    extends BaseFileEntry
{

    /**
     * Ensure consistent serialization.
     *
     * @since 1.0
     */
    private static final long serialVersionUID = 1L;

    /**
     * The path of the backing {@link Metadata}.
     *
     * @since 1.0
     */
    private final String path;

    /**
     * The backing {@link ArtifactStore}.
     *
     * @since 1.0
     */
    private final ArtifactStore store;

    /**
     * Creates a file entry for the specified parent directory of the specified file system that corresponds to the
     * specified metadata in the specified artifact store and will have the name
     * <code>maven-metadata.xml</code>.
     *
     * @param fileSystem the file system.
     * @param parent     the parent directory.
     * @param path       the path of the metadata.
     * @param store      the artifact store.
     * @since 1.0
     */
    public MetadataFileEntry( FileSystem fileSystem, DirectoryEntry parent, String path, ArtifactStore store )
    {
        super( fileSystem, parent, "maven-metadata.xml" );
        this.path = path;
        this.store = store;
    }

    /**
     * {@inheritDoc}
     */
    public long getSize()
        throws IOException
    {
        try
        {
            Metadata metadata = store.getMetadata( path );
            MetadataXpp3Writer writer = new MetadataXpp3Writer();
            StringWriter stringWriter = new StringWriter();
            writer.write( stringWriter, metadata );
            return stringWriter.toString().getBytes().length;
        }
        catch ( MetadataNotFoundException e )
        {
            throw new IOException( "File not found", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getInputStream()
        throws IOException
    {
        try
        {
            Metadata metadata = store.getMetadata( path );
            MetadataXpp3Writer writer = new MetadataXpp3Writer();
            StringWriter stringWriter = new StringWriter();
            writer.write( stringWriter, metadata );
            return new ByteArrayInputStream( stringWriter.toString().getBytes() );
        }
        catch ( MetadataNotFoundException e )
        {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified()
        throws IOException
    {
        try
        {
            return store.getMetadataLastModified( path );
        }
        catch ( MetadataNotFoundException e )
        {
            throw new IOException( "File not found", e);
        }
    }

}
