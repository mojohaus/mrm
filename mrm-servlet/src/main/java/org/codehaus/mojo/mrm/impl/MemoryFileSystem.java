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

public class MemoryFileSystem
    extends BaseFileSystem
{

    private final Map/*<DirectoryEntry,List<Entry>>*/ contents = new HashMap/*<DirectoryEntry,List<Entry>>*/();

    public MemoryFileSystem()
    {
        contents.put( getRoot(), new ArrayList() );
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Entry[] listEntries( DirectoryEntry directory )
    {
        List/*<Entry>*/ entries = (List/*<Entry>*/) contents.get( directory == null ? getRoot() : directory );
        if ( entries == null )
        {
            return null;
        }
        return (Entry[]) entries.toArray( new Entry[entries.size()] );
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified( DirectoryEntry entry )
        throws IOException
    {
        long lastModified = 0;
        Entry[] entries = listEntries( entry );
        if ( entries != null )
        {
            for ( int i = 0; i < entries.length; )
            {
                lastModified = Math.max( lastModified, entries[i].getLastModified() );
            }
        }
        return lastModified;
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized Entry get( DirectoryEntry parent, String name )
    {
        parent.getClass();
        List/*<Entry>*/ parentEntries = (List/*<Entry>*/) contents.get( parent );
        if ( parentEntries == null )
        {
            return null;
        }
        for ( Iterator/*<Entry>*/ i = parentEntries.iterator(); i.hasNext(); )
        {
            Entry entry = (Entry) i.next();
            if ( name.equals( entry.getName() ) )
            {
                return entry;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized DirectoryEntry mkdir( DirectoryEntry parent, String name )
    {
        parent.getClass();
        parent = getNormalizedParent( parent );
        List/*<Entry>*/ entries = getEntriesList( parent );
        for ( Iterator/*<Entry>*/ i = entries.iterator(); i.hasNext(); )
        {
            Entry entry = (Entry) i.next();
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
        parent.getClass();
        parent = getNormalizedParent( parent );
        List/*<Entry>*/ entries = getEntriesList( parent );
        for ( Iterator/*<Entry>*/ i = entries.iterator(); i.hasNext(); )
        {
            Entry entry = (Entry) i.next();
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

    private synchronized List getEntriesList( DirectoryEntry directory )
    {
        List entries;
        entries = (List/*<Entry>*/) contents.get( directory );
        if ( entries == null )
        {
            entries = new ArrayList/*<Entry>*/();
            contents.put( directory, entries );
        }
        return entries;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void remove( Entry entry )
    {
        List/*<Entry>*/ entries;
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
            entries = (List/*<Entry>*/) contents.get( parent );
            if ( entries == null )
            {
                return;
            }
        }
        for ( Iterator/*<Entry>*/ i = entries.iterator(); i.hasNext(); )
        {
            Entry e = (Entry) i.next();
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