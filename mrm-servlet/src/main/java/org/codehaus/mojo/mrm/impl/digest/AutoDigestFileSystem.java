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
import org.codehaus.mojo.mrm.impl.LinkFileEntry;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class AutoDigestFileSystem
    extends BaseFileSystem
{
    private final FileSystem backing;

    public AutoDigestFileSystem( FileSystem backing )
    {
        this.backing = backing;
    }

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
                if ( name.endsWith( ".md5" ) || name.endsWith( ".sha1" ) )
                {
                    present.add( name );
                }
                else
                {
                    missing.put( name + ".md5", entries[i] );
                    missing.put( name + ".sha1", entries[i] );
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
            if ( name.endsWith( ".md5" ) )
            {
                result.put( name, new MD5DigestFileEntry( this, directory, fileEntry ) );
            }
            else if ( name.endsWith( ".sha1" ) )
            {
                result.put( name, new SHA1DigestFileEntry( this, directory, fileEntry ) );
            }
        }
        return (Entry[]) result.values().toArray( new Entry[result.size()] );
    }

    public long getLastModified( DirectoryEntry entry )
        throws IOException
    {
        return backing.getLastModified( DefaultDirectoryEntry.equivalent( backing, entry ) );
    }

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
            if ( name.endsWith( ".md5" ) )
            {
                Entry shadow = backing.get( parent.toPath() + "/" + StringUtils.removeEnd( name, ".md5" ) );
                if ( shadow instanceof FileEntry )
                {
                    return new MD5DigestFileEntry( this, parent, (FileEntry) shadow );
                }
            }
            if ( name.endsWith( ".sha1" ) )
            {
                Entry shadow = backing.get( parent.toPath() + "/" + StringUtils.removeEnd( name, ".sha1" ) );
                if ( shadow instanceof FileEntry )
                {
                    return new SHA1DigestFileEntry( this, parent, (FileEntry) shadow );
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
                if ( entry.getName().endsWith( ".md5" ) )
                {
                    Entry shadow =
                        backing.get( parent.toPath() + "/" + StringUtils.removeEnd( entry.getName(), ".md5" ) );
                    if ( shadow instanceof FileEntry )
                    {
                        return new AutoMD5DigestFileEntry( this, parent, (FileEntry) shadow, (FileEntry) entry );
                    }
                }
                if ( entry.getName().endsWith( ".sha1" ) )
                {
                    Entry shadow =
                        backing.get( parent.toPath() + "/" + StringUtils.removeEnd( entry.getName(), ".sha1" ) );
                    if ( shadow instanceof FileEntry )
                    {
                        return new AutoSHA1DigestFileEntry( this, parent, (FileEntry) shadow, (FileEntry) entry );
                    }
                }
                return new LinkFileEntry( this, parent, (FileEntry) entry );
            }
            else if ( entry instanceof DirectoryEntry )
            {
                if ( entry.getName().endsWith( ".md5" ) )
                {
                    Entry shadow =
                        backing.get( parent.toPath() + "/" + StringUtils.removeEnd( entry.getName(), ".md5" ) );
                    if ( shadow instanceof FileEntry )
                    {
                        return new MD5DigestFileEntry( this, parent, (FileEntry) shadow );
                    }
                }
                if ( entry.getName().endsWith( ".sha1" ) )
                {
                    Entry shadow =
                        backing.get( parent.toPath() + "/" + StringUtils.removeEnd( entry.getName(), ".sha1" ) );
                    if ( shadow instanceof FileEntry )
                    {
                        return new SHA1DigestFileEntry( this, parent, (FileEntry) shadow );
                    }
                }
                return new DefaultDirectoryEntry( this, parent, entry.getName() );
            }
        }
        return null;
    }

}
