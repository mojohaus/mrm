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

package org.codehaus.mojo.mrm.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class BaseFileSystem
    implements FileSystem
{

    private final DirectoryEntry root = new DefaultDirectoryEntry( this, null, "" );

    public DirectoryEntry getRoot()
    {
        return root;
    }

    public Entry get( String path )
    {
        if ( path.startsWith( "/" ) )
        {
            path = path.substring( 1 );
        }
        if ( path.length() == 0 )
        {
            return root;
        }
        String[] parts = path.split( "/" );
        if ( parts.length == 0 )
        {
            return root;
        }
        DirectoryEntry parent = root;
        for ( int i = 0; i < parts.length - 1; i++ )
        {
            parent = new DefaultDirectoryEntry( this, parent, parts[i] );
        }
        return get( parent, parts[parts.length - 1] );
    }

    protected Entry get( DirectoryEntry parent, String name )
    {
        parent.getClass();
        Entry[] entries = listEntries( parent );
        if ( entries != null )
        {
            for ( int i = 0; i < entries.length; i++ )
            {
                if ( name.equals( entries[i].getName() ) )
                {
                    return entries[i];
                }
            }
        }
        return null;
    }

    public DirectoryEntry mkdir( DirectoryEntry parent, String name )
    {
        throw new UnsupportedOperationException();
    }

    public FileEntry put( DirectoryEntry parent, String name, InputStream content )
        throws IOException
    {
        throw new UnsupportedOperationException();
    }

    public FileEntry put( DirectoryEntry parent, String name, byte[] content )
        throws IOException
    {
        return put( parent, name, new ByteArrayInputStream( content ) );
    }

    public void remove( Entry entry )
    {
        throw new UnsupportedOperationException();
    }
}
