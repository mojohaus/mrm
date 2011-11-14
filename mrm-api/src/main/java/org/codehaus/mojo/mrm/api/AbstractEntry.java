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

import java.util.Stack;

/**
 * Abstract implementation of {@link Entry}.
 *
 * @serial
 * @since 1.0
 */
public abstract class AbstractEntry
    implements Entry
{

    /**
     * Ensure consistent serialization.
     *
     * @since 1.0
     */
    private static final long serialVersionUID = 1L;

    /**
     * The file system that this entry belongs to.
     *
     * @since 1.0
     */
    private final FileSystem fileSystem;

    /**
     * The directory that this entry belongs to.
     *
     * @since 1.0
     */
    private final DirectoryEntry parent;

    /**
     * The name of this entry.
     *
     * @since 1.0
     */
    private final String name;

    /**
     * Creates an entry in the specified file system with the specified parent and name.
     *
     * @param fileSystem The filesystem.
     * @param parent     The parent.
     * @param name       The name of the entry.
     * @since 1.0
     */
    protected AbstractEntry( FileSystem fileSystem, DirectoryEntry parent, String name )
    {
        this.fileSystem = fileSystem;
        this.parent = parent;
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public FileSystem getFileSystem()
    {
        return fileSystem;
    }

    /**
     * {@inheritDoc}
     */
    public DirectoryEntry getParent()
    {
        return parent;
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof AbstractEntry ) )
        {
            return false;
        }

        AbstractEntry abstractEntry = (AbstractEntry) o;

        if ( !fileSystem.equals( abstractEntry.fileSystem ) )
        {
            return false;
        }
        if ( !name.equals( abstractEntry.name ) )
        {
            return false;
        }
        if ( parent != null ? !parent.equals( abstractEntry.parent ) : abstractEntry.parent != null )
        {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public final int hashCode()
    {
        int result = name.hashCode();
        result = 31 * result + ( parent != null ? parent.hashCode() : 0 );
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append( "Entry[" );
        sb.append( fileSystem );
        sb.append( ':' ).append( toPath() ).append( ']' );
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public final String toPath()
    {
        Stack stack = new Stack();
        Entry root = getFileSystem().getRoot();
        Entry entry = this;
        while ( entry != null && !root.equals( entry ) )
        {
            stack.push( entry.getName() );
            entry = entry.getParent();
        }
        StringBuffer buf = new StringBuffer();
        while ( !stack.empty() )
        {
            buf.append( '/' );
            buf.append( stack.pop() );
        }
        return buf.toString();
    }


}
