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
import org.codehaus.mojo.mrm.api.BaseFileEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.FileEntry;
import org.codehaus.mojo.mrm.api.FileSystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A {@link FileEntry} that corresponds to the MD5 digest of another file entry.
 */
public class MD5DigestFileEntry
    extends BaseFileEntry
{

    /**
     * Ensure consistent serialization.
     *
     * @since 1.0
     */
    private static final long serialVersionUID = 1L;

    /**
     * The entry we will calculate the digest of.
     *
     * @since 1.0
     */
    private final FileEntry entry;

    /**
     * Creates an instance in the specified directory of the specified file system that will calculate the
     * digest of the specified file entry.
     *
     * @param fileSystem the file system.
     * @param parent     the parent directory.
     * @param entry      the entry to digest.
     * @since 1.0
     */
    public MD5DigestFileEntry( FileSystem fileSystem, DirectoryEntry parent, FileEntry entry )
    {
        super( fileSystem, parent, entry.getName() + ".md5" );
        this.entry = entry;
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified()
        throws IOException
    {
        return entry.getLastModified();
    }

    /**
     * {@inheritDoc}
     */
    public long getSize()
        throws IOException
    {
        return 32;
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getInputStream()
        throws IOException
    {
        return new ByteArrayInputStream( getContent() );
    }

    /**
     * Generates the digest.
     *
     * @return the digest.
     * @throws IOException if the backing entry could not be read.
     * @since 1.0
     */
    private byte[] getContent()
        throws IOException
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance( "MD5" );
            digest.reset();
            byte[] buffer = new byte[8192];
            int read;
            try (InputStream is = entry.getInputStream())
            {
                while ( ( read = is.read( buffer ) ) > 0 )
                {
                    digest.update( buffer, 0, read );
                }
            }
            final String md5 = StringUtils.leftPad( new BigInteger( 1, digest.digest() ).toString( 16 ), 32, "0" );
            return md5.getBytes();
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new IOException( "Unable to calculate hash", e);
        }
    }

    /**
     * A {@link org.codehaus.mojo.mrm.impl.digest.DigestFileEntryFactory} that creates MD5 digest entries.
     *
     * @since 1.0
     */
    public static class Factory
        extends BaseDigestFileEntryFactory
    {
        /**
         * {@inheritDoc}
         */
        public String getType()
        {
            return ".md5";
        }

        /**
         * {@inheritDoc}
         */
        public FileEntry create( FileSystem fileSystem, DirectoryEntry parent, FileEntry entry )
        {
            return new MD5DigestFileEntry( fileSystem, parent, entry );
        }
    }
}
