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

package org.codehaus.mojo.repository.api;

public abstract class BaseEntry
    implements Entry
{

    private final FileSystem fileSystem;

    private final DirectoryEntry parent;

    private final String name;

    protected BaseEntry( FileSystem fileSystem, DirectoryEntry parent, String name )
    {
        this.fileSystem = fileSystem;
        this.parent = parent;
        this.name = name;
    }

    public FileSystem getFileSystem()
    {
        return fileSystem;
    }

    public DirectoryEntry getParent()
    {
        return parent;
    }

    public String getName()
    {
        return name;
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof BaseEntry ) )
        {
            return false;
        }

        BaseEntry baseEntry = (BaseEntry) o;

        if ( !fileSystem.equals( baseEntry.fileSystem ) )
        {
            return false;
        }
        if ( !name.equals( baseEntry.name ) )
        {
            return false;
        }
        if ( parent != null ? !parent.equals( baseEntry.parent ) : baseEntry.parent != null )
        {
            return false;
        }

        return true;
    }

    public final int hashCode()
    {
        int result = name.hashCode();
        result = 31 * result + ( parent != null ? parent.hashCode() : 0 );
        return result;
    }
}
