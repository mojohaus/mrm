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

import org.apache.commons.io.IOUtils;
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

public class AutoSHA1DigestFileEntry
    extends BaseFileEntry
{

    private final FileEntry entry;

    private final FileEntry hashEntry;

    public AutoSHA1DigestFileEntry( FileSystem fileSystem, DirectoryEntry parent, FileEntry entry, FileEntry hashEntry )
    {
        super( fileSystem, parent, hashEntry.getName() );
        this.entry = entry;
        this.hashEntry = hashEntry;
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified()
        throws IOException
    {
        try
        {
            return hashEntry.getLastModified();
        }
        catch ( IOException e )
        {
            return entry.getLastModified();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getSize()
        throws IOException
    {
        return 40;
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getInputStream()
        throws IOException
    {
        try
        {
            return hashEntry.getInputStream();
        }
        catch ( IOException e )
        {
            return new ByteArrayInputStream( getContent() );
        }
    }

    private byte[] getContent()
        throws IOException
    {
        InputStream is = null;
        try
        {
            MessageDigest digest = MessageDigest.getInstance( "SHA1" );
            digest.reset();
            byte[] buffer = new byte[8192];
            int read;
            try
            {
                is = entry.getInputStream();
                while ( ( read = is.read( buffer ) ) > 0 )
                {
                    digest.update( buffer, 0, read );
                }
            }
            catch ( IOException e )
            {
                if ( is != null )
                {
                    throw e;
                }
            }
            final String md5 = StringUtils.leftPad( new BigInteger( 1, digest.digest() ).toString( 16 ), 40, "0" );
            return md5.getBytes();
        }
        catch ( NoSuchAlgorithmException e )
        {
            IOException ioe = new IOException( "Unable to calculate hash" );
            ioe.initCause( e );
            throw ioe;
        }
        finally
        {
            IOUtils.closeQuietly( is );
        }
    }
}
