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

package org.codehaus.mojo.repository.impl.digest;

import org.codehaus.mojo.repository.api.BaseFileSystem;
import org.codehaus.mojo.repository.api.DefaultDirectoryEntry;
import org.codehaus.mojo.repository.api.DirectoryEntry;
import org.codehaus.mojo.repository.api.Entry;
import org.codehaus.mojo.repository.api.FileEntry;
import org.codehaus.mojo.repository.api.FileSystem;
import org.codehaus.mojo.repository.impl.LinkFileEntry;

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
        Entry[] entries = backing.listEntries( equivalent( backing, directory ) );
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
                result.put( name, equivalent( this, (DirectoryEntry) entries[i] ) );
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
        return backing.getLastModified( equivalent( backing, entry ) );
    }

    private DirectoryEntry equivalent( FileSystem target, DirectoryEntry directory )
    {
        if ( directory.getParent() == null )
        {
            return target.getRoot();
        }
        return new DefaultDirectoryEntry( target, equivalent( target, directory.getParent() ), directory.getName() );
    }
}
