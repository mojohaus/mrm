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

import org.apache.commons.io.FileUtils;
import org.codehaus.mojo.mrm.api.BaseFileSystem;
import org.codehaus.mojo.mrm.api.DefaultDirectoryEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.Entry;
import org.codehaus.mojo.mrm.api.FileEntry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

public class DiskFileSystem
    extends BaseFileSystem
{
    private final File root;

    private final boolean readOnly;

    public DiskFileSystem( File root, boolean readOnly )
    {
        this.root = root;
        this.readOnly = readOnly;
    }

    public DiskFileSystem( File root )
    {
        this( root, true );
    }

    public Entry[] listEntries( DirectoryEntry directory )
    {
        File file = toFile( directory );
        if ( !file.isDirectory() )
        {
            return null;
        }
        File[] files = file.listFiles();
        Entry[] result = new Entry[files.length];
        for ( int i = 0; i < files.length; i++ )
        {
            if ( files[i].isFile() )
            {
                result[i] = new DiskFileEntry( this, directory, files[i] );
            }
            else
            {
                result[i] = new DefaultDirectoryEntry( this, directory, files[i].getName() );
            }
        }
        return result;
    }

    public long getLastModified( DirectoryEntry entry )
        throws IOException
    {
        return toFile( entry ).lastModified();
    }

    private File toFile( Entry entry )
    {
        Stack stack = new Stack();
        Entry root = getRoot();
        while ( entry != null && !root.equals( entry ) )
        {
            String name = entry.getName();
            if ( "..".equals( name ) )
            {
                if ( !stack.isEmpty() )
                {
                    stack.pop();
                }
            }
            else if ( !".".equals( name ) )
            {
                stack.push( name );
            }
            entry = entry.getParent();
        }
        File file = this.root;
        while ( !stack.empty() )
        {
            file = new File( file, (String) stack.pop() );
        }
        return file;
    }

    public DirectoryEntry mkdir( DirectoryEntry parent, String name )
    {
        if ( readOnly )
        {
            throw new UnsupportedOperationException();
        }
        File file = new File( toFile( parent ), name );
        file.mkdirs();
        return new DefaultDirectoryEntry( this, parent, name );
    }

    public FileEntry put( DirectoryEntry parent, String name, InputStream content )
        throws IOException
    {
        if ( readOnly )
        {
            throw new UnsupportedOperationException();
        }
        File parentFile = toFile( parent );
        parentFile.mkdirs();
        File file = new File( parentFile, name );
        FileUtils.copyInputStreamToFile( content, file );
        return new DiskFileEntry( this, parent, file );
    }

    public void remove( Entry entry )
    {
        if ( readOnly )
        {
            throw new UnsupportedOperationException();
        }
        File file = toFile( entry );
        file.delete();
    }
}
