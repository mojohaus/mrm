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

import org.apache.commons.io.IOUtils;
import org.codehaus.mojo.mrm.api.BaseFileSystem;
import org.codehaus.mojo.mrm.api.DefaultDirectoryEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.Entry;
import org.codehaus.mojo.mrm.api.FileEntry;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A file system who's structure is entirely stored in memory.
 *
 * @since 1.0
 */
public class MemoryFileSystem
    extends BaseFileSystem
{

    /**
     * The file system content.
     *
     * @since 1.0
     */
    private final Map<DirectoryEntry, List<Entry>> contents = new HashMap<>();

    /**
     * Create a new empty file system.
     *
     * @since 1.0
     */
    public MemoryFileSystem()
    {
        contents.put( getRoot(), new ArrayList<>() );
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Entry[] listEntries( DirectoryEntry directory )
    {
        List<Entry> entries = contents.get( directory == null ? getRoot() : directory );
        if ( entries == null )
        {
            return null;
        }
        return entries.toArray(new Entry[0]);
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified( DirectoryEntry directoryEntry )
        throws IOException
    {
        long lastModified = 0;
        Entry[] entries = listEntries( directoryEntry );
        if ( entries != null )
        {
            for ( Entry entry : entries )
            {
                lastModified = Math.max( lastModified, entry.getLastModified() );
            }
        }

        return lastModified;
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized Entry get( DirectoryEntry parent, String name )
    {
        List<Entry> parentEntries = contents.get( parent );
        return parentEntries == null ? null :
                parentEntries.stream().filter(entry -> name.equals(entry.getName())).findFirst().orElse(null);

    }

    /**
     * {@inheritDoc}
     */
    public synchronized DirectoryEntry mkdir( DirectoryEntry parent, String name )
    {
        parent = getNormalizedParent( parent );
        List<Entry> entries = getEntriesList( parent );
        for ( Entry entry : entries )
        {
            if ( name.equals( entry.getName() ) )
            {
                if ( entry instanceof DirectoryEntry )
                {
                    return (DirectoryEntry) entry;
                }
                return null;
            }
        }
        DirectoryEntry entry = new DefaultDirectoryEntry( this, parent, name );
        entries.add( entry );
        return entry;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized FileEntry put( DirectoryEntry parent, String name, InputStream content )
        throws IOException
    {
        parent = getNormalizedParent( parent );
        List<Entry> entries = getEntriesList( parent );
        for ( Iterator<Entry> i = entries.iterator(); i.hasNext(); )
        {
            Entry entry = i.next();
            if ( name.equals( entry.getName() ) )
            {
                if ( entry instanceof FileEntry )
                {
                    i.remove();
                }
                else
                {
                    return null;
                }
            }
        }
        FileEntry entry = new MemoryFileEntry( this, parent, name, IOUtils.toByteArray( content ) );
        entries.add( entry );
        return entry;
    }

    /**
     * Gets the parent directory entry (ensuring it exists).
     *
     * @param parent the parent entry to get.
     * @return the actual directory entry instance used as the key in {@link #contents}.
     * @since 1.0
     */
    private DirectoryEntry getNormalizedParent( DirectoryEntry parent )
    {
        if ( parent.getParent() == null )
        {
            return getRoot();
        }
        else
        {
            return mkdir( parent.getParent(), parent.getName() );
        }
    }

    /**
     * Gets the list of entries in the specified directory.
     *
     * @param directory the directory to get the entries of.
     * @return the list of entries (never <code>null</code>).
     * @since 1.0
     */
    private synchronized List<Entry> getEntriesList( DirectoryEntry directory )
    {
        return contents.computeIfAbsent(directory, k -> new ArrayList<>());
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void remove( Entry entry )
    {
        List<Entry> entries;
        if ( entry == null )
        {
            return;
        }
        DirectoryEntry parent = entry.getParent();
        if ( parent == null )
        {
            return;
        }
        else
        {
            entries = contents.get( parent );
            if ( entries == null )
            {
                return;
            }
        }
        for ( Iterator<Entry> i = entries.iterator(); i.hasNext(); )
        {
            Entry e = i.next();
            if ( entry.equals( e ) )
            {
                if ( e instanceof DirectoryEntry )
                {
                    Entry[] children = listEntries( (DirectoryEntry) e );
                    for ( int j = children.length - 1; j >= 0; j-- )
                    {
                        remove( children[j] );
                    }
                    contents.remove( e );
                }
                i.remove();
                return;
            }
        }
    }
}