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

import org.codehaus.mojo.mrm.api.DefaultDirectoryEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.Entry;
import org.codehaus.mojo.mrm.api.FileEntry;
import org.codehaus.mojo.mrm.api.FileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

/**
 * A file system that is a composite of multiple file systems.
 *
 * @since 1.0
 */
public class CompositeFileSystem
    implements FileSystem
{

    /**
     * Ensure consistent serialization.
     *
     * @since 1.0
     */
    private static final long serialVersionUID = 1L;

    /**
     * The file systems to delegate to (in order of preference).
     *
     * @since 1.0
     */
    private final FileSystem[] delegates;

    /**
     * The root entry.
     *
     * @since 1.0
     */
    private final DirectoryEntry root = new DefaultDirectoryEntry( this, null, "" );

    /**
     * Creates a new {@link FileSystem} that will delegate to each of the supplied delegate {@link FileSystem} in turn
     * until a matching entry is found.
     *
     * @param delegates the delegate {@link FileSystem}s (in order of preference).
     */
    public CompositeFileSystem( FileSystem[] delegates )
    {
        delegates.getClass();
        this.delegates = delegates;
    }

    /**
     * {@inheritDoc}
     */
    public Entry[] listEntries( DirectoryEntry directory )
    {
        Map/*<String,Entry>*/ result = new TreeMap();
        for ( int i = 0; i < delegates.length; i++ )
        {
            Entry[] entries = delegates[i].listEntries( DefaultDirectoryEntry.equivalent( delegates[i], directory ) );
            if ( entries == null )
            {
                continue;
            }
            for ( int j = 0; j < entries.length; j++ )
            {
                if ( result.containsKey( entries[j].getName() ) )
                {
                    continue;
                }
                if ( entries[j] instanceof DirectoryEntry )
                {
                    result.put( entries[j].getName(),
                                new DefaultDirectoryEntry( this, directory, entries[j].getName() ) );
                }
                else if ( entries[j] instanceof FileEntry )
                {
                    result.put( entries[j].getName(), new LinkFileEntry( this, directory, (FileEntry) entries[j] ) );
                }
            }
        }
        return (Entry[]) result.values().toArray( new Entry[result.size()] );
    }

    /**
     * {@inheritDoc}
     */
    public Entry get( String path )
    {
        for ( int i = 0; i < delegates.length; i++ )
        {
            Entry entry = delegates[i].get( path );
            if ( entry == null )
            {
                continue;
            }
            if ( entry instanceof DirectoryEntry )
            {
                return DefaultDirectoryEntry.equivalent( this, (DirectoryEntry) entry );
            }
            return entry;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified( DirectoryEntry entry )
        throws IOException
    {
        long lastModified = 0;
        for ( int i = 0; i < delegates.length; i++ )
        {
            try
            {
                lastModified = Math.max( lastModified, delegates[i].getLastModified( entry ) );
            }
            catch ( IOException e )
            {
                // ignore
            }
        }
        return lastModified;
    }

    /**
     * {@inheritDoc}
     */
    public DirectoryEntry getRoot()
    {
        return root;
    }

    /**
     * {@inheritDoc}
     */
    public DirectoryEntry mkdir( DirectoryEntry parent, String name )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public FileEntry put( DirectoryEntry parent, String name, InputStream content )
        throws IOException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public FileEntry put( DirectoryEntry parent, String name, byte[] content )
        throws IOException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void remove( Entry entry )
    {
        throw new UnsupportedOperationException();
    }

}
