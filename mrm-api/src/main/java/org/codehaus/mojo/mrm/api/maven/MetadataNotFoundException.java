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

package org.codehaus.mojo.mrm.api.maven;

/**
 * An exception that indicates that an artifact could not be found.
 *
 * @since 1.0
 */
public class MetadataNotFoundException
    extends Exception
{
    /**
     * Ensure consistent serialization.
     *
     * @since 1.0
     */
    private static final long serialVersionUID = 1L;

    /**
     * The path.
     *
     * @since 1.0
     */
    private final String path;

    /**
     * Creates a new {@link MetadataNotFoundException}.
     *
     * @param path the path of the metadata that was not found.
     * @since 1.0
     */
    public MetadataNotFoundException( String path )
    {
        this( path, path );
    }

    /**
     * Creates a new {@link MetadataNotFoundException}.
     *
     * @param message The message.
     * @param path    the path of the metadata that was not found.
     * @since 1.0
     */
    public MetadataNotFoundException( String message, String path )
    {
        this( message, path, null );
    }

    /**
     * Creates a new {@link MetadataNotFoundException}.
     *
     * @param path  the path of the metadata that was not found.
     * @param cause the reason why it was not found (or <code>null</code> if there is no specific reason)
     * @since 1.0
     */
    public MetadataNotFoundException( String path, Throwable cause )
    {
        this( path, path, cause );
    }

    /**
     * Creates a new {@link MetadataNotFoundException}.
     *
     * @param message The message.
     * @param path    the path of the metadata that was not found.
     * @param cause   the reason why it was not found (or <code>null</code> if there is no specific reason)
     * @since 1.0
     */
    public MetadataNotFoundException( String message, String path, Throwable cause )
    {
        super( message, cause );
        this.path = path;
    }

    /**
     * Gets the path of the metadata that does not exist.
     *
     * @return the path of the metadata that was not found.
     * @since 1.0
     */
    public String getPath()
    {
        return path;
    }
}
