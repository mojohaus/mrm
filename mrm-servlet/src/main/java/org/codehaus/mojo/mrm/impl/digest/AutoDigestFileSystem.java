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

package org.codehaus.mojo.mrm.impl.digest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.mojo.mrm.api.BaseFileSystem;
import org.codehaus.mojo.mrm.api.DefaultDirectoryEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.Entry;
import org.codehaus.mojo.mrm.api.FileEntry;
import org.codehaus.mojo.mrm.api.FileSystem;
import org.codehaus.mojo.mrm.impl.GenerateOnErrorFileEntry;
import org.codehaus.mojo.mrm.impl.LinkFileEntry;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A delegating file system that will automatically provide digests of any files that are missing digests from
 * the backing file system.
 *
 * @since 1.0
 */
public class AutoDigestFileSystem
    extends BaseFileSystem
{
    /**
     * The backing filesystem.
     *
     * @since 1.0
     */
    private final FileSystem backing;

    /**
     * The digest factories that we will use.
     *
     * @since 1.0
     */
    private final Map/*<String,DigestFileEntryFactory>*/ digestFactories;

    /**
     * Creates an instance that will add SHA1 and MD5 digests to the backing file system for any entries that are
     * missing digests.
     *
     * @param backing the backing file system.
     * @since 1.0
     */
    public AutoDigestFileSystem( FileSystem backing )
    {
        this( backing,
              new DigestFileEntryFactory[]{ new MD5DigestFileEntry.Factory(), new SHA1DigestFileEntry.Factory() } );
    }

    /**
     * Creates an instance that will use the supplied {@link DigestFileEntryFactory}s to add any missing digests to the
     * backing file system.
     *
     * @param backing         the backing file system.
     * @param digestFactories the digest factories.
     * @since 1.0
     */
    public AutoDigestFileSystem( FileSystem backing, DigestFileEntryFactory[] digestFactories )
    {
        this.backing = backing;
        Map map = new HashMap( digestFactories.length );
        for ( int i = 0; i < digestFactories.length; i++ )
        {
            map.put( digestFactories[i].getType(), digestFactories[i] );
        }
        this.digestFactories = Collections.unmodifiableMap( map );
    }

    /**
     * {@inheritDoc}
     */
    public Entry[] listEntries( DirectoryEntry directory )
    {
        Map result = new TreeMap();
        Map missing = new HashMap();
        Set present = new HashSet();
        Entry[] entries = backing.listEntries( DefaultDirectoryEntry.equivalent( backing, directory ) );
        for ( int i = 0; i < entries.length; i++ )
        {
            final String name = entries[i].getName();
            if ( entries[i] instanceof FileEntry )
            {
                for ( Iterator/*<String>*/ j = digestFactories.keySet().iterator(); j.hasNext(); )
                {
                    String type = (String) j.next();
                    if ( name.endsWith( type ) )
                    {
                        present.add( name );
                    }
                    else
                    {
                        missing.put( name + type, entries[i] );
                    }
                }
                result.put( name, new LinkFileEntry( this, directory, (FileEntry) entries[i] ) );
            }
            else if ( entries[i] instanceof DirectoryEntry )
            {
                result.put( name, DefaultDirectoryEntry.equivalent( this, (DirectoryEntry) entries[i] ) );
            }
        }
        missing.keySet().removeAll( present );
        for ( Iterator i = missing.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();
            FileEntry fileEntry = (FileEntry) entry.getValue();
            for ( Iterator/*<DigestFileEntryFactory>*/ j = digestFactories.values().iterator(); j.hasNext(); )
            {
                DigestFileEntryFactory factory = (DigestFileEntryFactory) j.next();
                if ( name.endsWith( factory.getType() ) )
                {
                    result.put( name, factory.create( this, directory, fileEntry ) );
                }
            }
        }
        return (Entry[]) result.values().toArray( new Entry[result.size()] );
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified( DirectoryEntry entry )
        throws IOException
    {
        return backing.getLastModified( DefaultDirectoryEntry.equivalent( backing, entry ) );
    }

    /**
     * {@inheritDoc}
     */
    public Entry get( String path )
    {
        Entry entry = backing.get( path );
        if ( entry == null )
        {
            if ( path.startsWith( "/" ) )
            {
                path = path.substring( 1 );
            }
            if ( path.length() == 0 )
            {
                return getRoot();
            }
            String[] parts = path.split( "/" );
            if ( parts.length == 0 )
            {
                return getRoot();
            }
            DirectoryEntry parent = getRoot();
            for ( int i = 0; i < parts.length - 1; i++ )
            {
                parent = new DefaultDirectoryEntry( this, parent, parts[i] );
            }
            String name = parts[parts.length - 1];
            for ( Iterator/*<DigestFileEntryFactory>*/ j = digestFactories.values().iterator(); j.hasNext(); )
            {
                DigestFileEntryFactory factory = (DigestFileEntryFactory) j.next();
                if ( name.endsWith( factory.getType() ) )
                {
                    Entry shadow =
                        backing.get( parent.toPath() + "/" + StringUtils.removeEnd( name, factory.getType() ) );
                    return factory.create( this, parent, (FileEntry) shadow );
                }
            }
            return get( parent, name );
        }
        else
        {
            if ( path.startsWith( "/" ) )
            {
                path = path.substring( 1 );
            }
            if ( path.length() == 0 )
            {
                return getRoot();
            }
            String[] parts = path.split( "/" );
            if ( parts.length == 0 )
            {
                return getRoot();
            }
            DirectoryEntry parent = getRoot();
            for ( int i = 0; i < parts.length - 1; i++ )
            {
                parent = new DefaultDirectoryEntry( this, parent, parts[i] );
            }
            if ( entry instanceof FileEntry )
            {
                // repair filesystems that lie to us because they are caching
                for ( Iterator/*<DigestFileEntryFactory>*/ j = digestFactories.values().iterator(); j.hasNext(); )
                {
                    DigestFileEntryFactory factory = (DigestFileEntryFactory) j.next();
                    if ( entry.getName().endsWith( factory.getType() ) )
                    {
                        Entry shadow = backing.get(
                            parent.toPath() + "/" + StringUtils.removeEnd( entry.getName(), factory.getType() ) );
                        return new GenerateOnErrorFileEntry( this, parent, (FileEntry) entry,
                                                             factory.create( this, parent, (FileEntry) shadow ) );
                    }
                }
                return new LinkFileEntry( this, parent, (FileEntry) entry );
            }
            else if ( entry instanceof DirectoryEntry )
            {
                for ( Iterator/*<DigestFileEntryFactory>*/ j = digestFactories.values().iterator(); j.hasNext(); )
                {
                    DigestFileEntryFactory factory = (DigestFileEntryFactory) j.next();
                    if ( entry.getName().endsWith( factory.getType() ) )
                    {
                        Entry shadow = backing.get(
                            parent.toPath() + "/" + StringUtils.removeEnd( entry.getName(), factory.getType() ) );
                        return factory.create( this, parent, (FileEntry) shadow );
                    }
                }
                return new DefaultDirectoryEntry( this, parent, entry.getName() );
            }
        }
        return null;
    }

}
