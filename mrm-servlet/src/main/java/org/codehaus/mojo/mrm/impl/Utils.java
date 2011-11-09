/*
 * Copyright 2009-2011 Stephen Connolly
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

import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Utility class.
 */
public final class Utils
{
    /**
     * Do not instantiate.
     */
    private Utils()
    {
        throw new IllegalAccessError( "Utility class" );
    }

    /**
     * Returns an input stream for the specified content.  If the content is a byte array, the input stream will be a
     * {@link java.io.ByteArrayInputStream} if the content is a File, the input stream will be a
     * {@link java.io.FileInputStream} otherwise the content will be converted to a String and then into its UTF-8
     * representation and a {@link java.io.ByteArrayInputStream} returned.
     *
     * @param content The content.
     * @return an input stream of the content.
     * @throws java.io.IOException if things go wrong.
     */
    public static InputStream asInputStream( Object content )
        throws IOException
    {
        if ( content instanceof byte[] )
        {
            return new ByteArrayInputStream( (byte[]) content );
        }
        else if ( content instanceof File )
        {
            return new FileInputStream( (File) content );
        }
        else
        {
            return new ByteArrayInputStream( content.toString().getBytes( "UTF-8" ) );
        }
    }

    /**
     * Creates an empty jar file.
     *
     * @return the empty jar file as a byte array.
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          if things go wrong.
     */
    public static byte[] newEmptyJarContent()
        throws MojoExecutionException
    {
        byte[] emptyJar;
        try
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final Manifest manifest = new Manifest();
            manifest.getMainAttributes().putValue( "Manifest-Version", "1.0" );
            manifest.getMainAttributes().putValue( "Archiver-Version", "1.0" );
            manifest.getMainAttributes().putValue( "Created-By", "Mock Repository Maven Plugin" );
            JarOutputStream jos = new JarOutputStream( bos, manifest );
            jos.close();
            bos.close();
            emptyJar = bos.toByteArray();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not create empty jar file", e );
        }
        return emptyJar;
    }

    /**
     * Converts a GAV coordinate into the base file path and name for all artifacts at that coordinate.
     *
     * @param groupId    the group id.
     * @param artifactId the artifact id.
     * @param version    the version.
     * @return the base filepath for artifacts at the specified coordinates.
     */
    public static String getGAVPathName( String groupId, String artifactId, String version )
    {
        return getGAVPath( groupId, artifactId, version ) + '/' + artifactId + '-' + version;
    }

    public static String getGAVPath( String groupId, String artifactId, String version )
    {
        return groupId.replace( '.', '/' ) + ( artifactId != null ? ( '/' + artifactId + ( version != null ? ( '/'
            + version ) : "" ) ) : "" );
    }

    public static String getVersion( Model model )
    {
        String version = model.getVersion();
        if ( version == null )
        {
            version = model.getParent().getVersion();
        }
        return version;
    }

    public static String getArtifactId( Model model )
    {
        String artifactId = model.getArtifactId();
        if ( artifactId == null )
        {
            artifactId = model.getParent().getArtifactId();
        }
        return artifactId;
    }

    public static String getGroupId( Model model )
    {
        String groupId = model.getGroupId();
        if ( groupId == null )
        {
            groupId = model.getParent().getGroupId();
        }
        return groupId;
    }

    public static String urlEncodePath( String path )
        throws UnsupportedEncodingException
    {
        StringBuffer buf = new StringBuffer( path.length() + 64 );
        int last = 0;
        for ( int i = path.indexOf( '/' ); i != -1; i = path.indexOf( '/', last ) )
        {
            buf.append( urlEncodePathSegment( path.substring( last, i ) ) );
            buf.append( path.substring( i, Math.min( path.length(), i + 1 ) ) );
            last = i + 1;
        }
        buf.append( path.substring( last ) );
        return buf.toString();
    }

    public static String urlEncodePathSegment( String pathSegment )
        throws UnsupportedEncodingException
    {
        StringBuffer buf = new StringBuffer( pathSegment.length() + 64 );
        byte[] chars = pathSegment.getBytes( "UTF-8" );
        for ( int i = 0; i < chars.length; i++ )
        {
            switch ( chars[i] )
            {
                case '$':
                case '-':
                case '_':
                case '.':
                case '!':
                case '*':
                case '\'':
                case '(':
                case ')':
                case ',':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'G':
                case 'H':
                case 'I':
                case 'J':
                case 'K':
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                case 'Q':
                case 'R':
                case 'S':
                case 'T':
                case 'U':
                case 'V':
                case 'W':
                case 'X':
                case 'Y':
                case 'Z':
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'n':
                case 'o':
                case 'p':
                case 'q':
                case 'r':
                case 's':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                    buf.append( (char) chars[i] );
                    break;
                case ' ':
                    buf.append( '+' );
                    break;
                default:
                    buf.append( '%' );
                    if ( ( chars[i] & 0xf0 ) == 0 )
                    {
                        buf.append( '0' );
                    }
                    buf.append( Integer.toHexString( chars[i] & 0xff ) );
                    break;
            }
        }
        return buf.toString();
    }
}
