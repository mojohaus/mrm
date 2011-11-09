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

package org.codehaus.mojo.repository.servlet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.mojo.repository.api.DefaultDirectoryEntry;
import org.codehaus.mojo.repository.api.DirectoryEntry;
import org.codehaus.mojo.repository.api.Entry;
import org.codehaus.mojo.repository.api.FileEntry;
import org.codehaus.mojo.repository.api.FileSystem;
import org.codehaus.mojo.repository.api.maven.Artifact;
import org.codehaus.mojo.repository.api.maven.ArtifactStore;
import org.codehaus.mojo.repository.api.maven.ArtifactStoreFileSystem;
import org.codehaus.mojo.repository.impl.CompositeFileSystem;
import org.codehaus.mojo.repository.impl.DiskFileSystem;
import org.codehaus.mojo.repository.impl.MemoryFileSystem;
import org.codehaus.mojo.repository.impl.Utils;
import org.codehaus.mojo.repository.impl.digest.AutoDigestFileSystem;
import org.codehaus.mojo.repository.impl.maven.CompositeArtifactStore;
import org.codehaus.mojo.repository.impl.maven.FileSystemArtifactStore;
import org.codehaus.mojo.repository.impl.maven.MemoryArtifactStore;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileSystemServlet
    extends HttpServlet
{

    private FileSystem fileSystem;

    public void init( ServletConfig config )
        throws ServletException
    {
        super.init( config );

        // TODO rip out this and instantiate via configuration

        FileSystem fs1 = new MemoryFileSystem();
        DirectoryEntry root = fs1.getRoot();
        DirectoryEntry foo = fs1.mkdir( root, "foo" );
        try
        {
            fs1.put( root, "README", "This is a file".getBytes() );
            fs1.put( foo, "README.txt", "This is another file".getBytes() );
        }
        catch ( IOException e )
        {
            // ignore
        }
        FileSystem fs2 = new MemoryFileSystem();
        root = fs2.getRoot();
        foo = fs2.mkdir( root, "foo" );
        try
        {
            fs2.put( root, "README", "This is a hidden file".getBytes() );
            fs2.put( root, "HELP", "This is a lower layer file".getBytes() );
            fs2.put( foo, "README.txt", "This is a hidden file".getBytes() );
            fs2.put( foo, "HELP", "This is a lower layer file".getBytes() );
        }
        catch ( IOException e )
        {
            // ignore
        }
        ArtifactStore store = new MemoryArtifactStore();
        try
        {
            store.set( new Artifact( "foo.bar.manchu", "manchu-impl", "1.0", "pom" ), new ByteArrayInputStream(
                "<project><groupId>foo.bar.machu</groupId><artifactId>manchu-impl</artifactId><version>1.0</version></project>".getBytes() ) );
            store.set( new Artifact( "foo.bar.manchu", "manchu-impl", "1.1", "pom" ), new ByteArrayInputStream(
                "<project><groupId>foo.bar.machu</groupId><artifactId>manchu-impl</artifactId><version>1.1</version></project>".getBytes() ) );
            store.set( new Artifact( "foo.bar.manchu", "manchu-impl", "1.2", "pom" ), new ByteArrayInputStream(
                "<project><groupId>foo.bar.machu</groupId><artifactId>manchu-impl</artifactId><version>1.2</version></project>".getBytes() ) );
            store.set( new Artifact( "foo.bar.manchu", "manchu-maven-plugin", "1-SNAPSHOT", "pom" ),
                       new ByteArrayInputStream(
                           "<project><groupId>foo.bar.machu</groupId><artifactId>manchu-maven-plugin</artifactId><version>1-SNAPSHOT</version><packaging>maven-plugin</packaging></project>".getBytes() ) );
        }
        catch ( IOException e )
        {
            // ignore
        }
        ArtifactStore store2 = new MemoryArtifactStore();
        try
        {
            store2.set( new Artifact( "foo.bar.manchu", "manchu-impl", "0.9", "pom" ), new ByteArrayInputStream(
                "<project><groupId>foo.bar.machu</groupId><artifactId>manchu-impl</artifactId><version>0.9</version></project>".getBytes() ) );
            store2.set( new Artifact( "foo.bar.manchu", "manchu-impl", "1.0", "pom" ), new ByteArrayInputStream(
                "<project><groupId>foo.bar.machu</groupId><artifactId>manchu-impl</artifactId><version>1.0</version></project>".getBytes() ) );
            store2.set( new Artifact( "foo.bar.manchu", "manchu-impl", "1.3", "pom" ), new ByteArrayInputStream(
                "<project><groupId>foo.bar.machu</groupId><artifactId>manchu-impl</artifactId><version>1.3</version></project>".getBytes() ) );
            store2.set( new Artifact( "foo.bar.manchu", "manchu-maven-plugin", "2-SNAPSHOT", "pom",
                                      System.currentTimeMillis() - 2000, 1 ), new ByteArrayInputStream(
                "<project><groupId>foo.bar.machu</groupId><artifactId>manchu-maven-plugin</artifactId><version>2-SNAPSHOT</version><packaging>maven-plugin</packaging></project>".getBytes() ) );
            store2.set( new Artifact( "foo.bar.manchu", "manchu-maven-plugin", "2-SNAPSHOT", "pom",
                                      System.currentTimeMillis() - 1000, 2 ), new ByteArrayInputStream(
                "<project><groupId>foo.bar.machu</groupId><artifactId>manchu-maven-plugin</artifactId><version>2-SNAPSHOT</version><packaging>maven-plugin</packaging></project>".getBytes() ) );
            store2.set(
                new Artifact( "foo.bar.manchu", "manchu-maven-plugin", "2-SNAPSHOT", "pom", System.currentTimeMillis(),
                              3 ), new ByteArrayInputStream(
                "<project><groupId>foo.bar.machu</groupId><artifactId>manchu-maven-plugin</artifactId><version>2-SNAPSHOT</version><packaging>maven-plugin</packaging></project>".getBytes() ) );
            store2.set( new Artifact( "foo.bar.manchu", "manchu2-maven-plugin", "1.0", "pom" ),
                        new ByteArrayInputStream(
                            "<project><groupId>foo.bar.machu</groupId><artifactId>manchu2-maven-plugin</artifactId><version>2-SNAPSHOT</version><packaging>maven-plugin</packaging></project>".getBytes() ) );
        }
        catch ( IOException e )
        {
            // ignore
        }
        fileSystem = new AutoDigestFileSystem( new CompositeFileSystem( new FileSystem[]{ fs1, fs2,
            new ArtifactStoreFileSystem( new CompositeArtifactStore( new ArtifactStore[]{ store, store2,
                new FileSystemArtifactStore(
                    new DiskFileSystem( new File( "/Users/stephenc/.m2/repository" ) ) ) } ) ) } ) );
    }

    protected void doGet( HttpServletRequest req, HttpServletResponse resp )
        throws ServletException, IOException
    {
        String path = req.getPathInfo();
        String context;
        if ( path == null )
        {
            path = req.getServletPath();
            context = req.getContextPath();
        }
        else
        {
            context = req.getContextPath() + req.getServletPath();
        }
        Entry entry = fileSystem.get( path );
        if ( entry instanceof FileEntry )
        {
            FileEntry fileEntry = (FileEntry) entry;
            long size = fileEntry.getSize();
            if ( size >= 0 && size < Integer.MAX_VALUE )
            {
                resp.setContentLength( (int) size );
            }
            resp.setContentType( getServletContext().getMimeType( fileEntry.getName() ) );
            InputStream source = null;
            OutputStream destination = null;
            try
            {
                source = fileEntry.getInputStream();
                destination = resp.getOutputStream();
                IOUtils.copy( source, destination );
            }
            finally
            {
                IOUtils.closeQuietly( source );
                IOUtils.closeQuietly( destination );
            }
            return;
        }
        else if ( entry instanceof DirectoryEntry )
        {
            if ( !path.endsWith( "/" ) )
            {
                resp.sendRedirect( entry.getName() + "/" );
                return;
            }
            DirectoryEntry dirEntry = (DirectoryEntry) entry;
            Entry[] entries = fileSystem.listEntries( dirEntry );
            resp.setContentType( "text/html" );
            PrintWriter w = resp.getWriter();
            w.println( "<html>" );
            w.println( "  <head>" );
            w.println( "    <title>Index of " + context + path + "</title>" );
            w.println( "    <meta http-equiv=\"Content-Type\" repository=\"text/html; charset=utf-8\"/>" );
            w.println( "</head>" );
            w.println( "<body>" );
            w.println( "<h1>Index of " + context + path + "</h1>" );
            w.println( "  <hr/>" );
            w.write( "<pre>" );

            if ( dirEntry.getParent() != null )
            {
                w.println( "<a href='../'>../</a>" );
            }
            SimpleDateFormat format = new SimpleDateFormat( "dd-MMM-yyyy hh:mm" );
            if ( entries != null )
            {
                for ( int i = 0; i < entries.length; i++ )
                {
                    final String childName = entries[i].getName();
                    boolean directory = entries[i] instanceof DirectoryEntry;
                    if ( directory )
                    {
                        w.write( "<a href=\"./" + Utils.urlEncodePathSegment( childName ) + "/\">"
                                     + formatName( childName + "/" ) + "</a>" + StringUtils.repeat( " ", Math.max( 0, 49
                            - childName.length() ) ) );
                    }
                    else
                    {
                        w.write(
                            "<a href=\"./" + Utils.urlEncodePathSegment( childName ) + "\">" + formatName( childName )
                                + "</a>" + StringUtils.repeat( " ", Math.max( 0, 50 - childName.length() ) ) );
                    }

                    long timestamp = 0;
                    try
                    {
                        timestamp = entries[i].getLastModified();
                    }
                    catch ( IOException e )
                    {
                        // ignore
                    }

                    w.write( " " );
                    w.write( format.format( timestamp != -1 ? new Date( timestamp ) : new Date() ) );
                    if ( directory )
                    {
                        w.println( StringUtils.leftPad( "-", 20 ) );
                    }
                    else if ( entries[i] instanceof FileEntry )
                    {
                        FileEntry fileEntry = (FileEntry) entries[i];
                        try
                        {
                            long size = fileEntry.getSize();
                            if ( size >= 0 )
                            {
                                w.println( StringUtils.leftPad( Long.toString( size ), 20 ) );
                            }
                            else
                            {
                                w.println( StringUtils.leftPad( "-", 20 ) );
                            }
                        }
                        catch ( IOException e )
                        {
                            w.println( StringUtils.leftPad( "-", 20 ) );
                        }
                    }
                    else
                    {
                        w.println( StringUtils.leftPad( "-", 20 ) );
                    }
                }
            }
            w.write( "</pre>" );
            w.println( "  <hr/>" );
            w.println( "</body>" );
            w.println( "</html>" );
            return;
        }

        resp.sendError( 404 );
    }

    protected void doPut( HttpServletRequest req, HttpServletResponse resp )
        throws ServletException, IOException
    {
        String path = req.getPathInfo();
        String context;
        if ( path == null )
        {
            path = req.getServletPath();
            context = req.getContextPath();
        }
        else
        {
            context = req.getContextPath() + req.getServletPath();
        }
        if ( path.endsWith( "/" ) )
        {
            resp.sendError( 405 );
            return;
        }
        if ( path.startsWith( "/" ) )
        {
            path = path.substring( 1 );
        }
        String[] parts = path.split( "/" );
        if ( parts.length == 0 )
        {
            resp.sendError( 405 );
            return;
        }
        String name = parts[parts.length - 1];
        if ( StringUtils.isEmpty( name ) )
        {
            resp.sendError( 405 );
            return;
        }
        DirectoryEntry parent = fileSystem.getRoot();
        for ( int i = 0; i < parts.length - 1; i++ )
        {
            parent = new DefaultDirectoryEntry( fileSystem, parent, parts[i] );
        }
        ServletInputStream inputStream = null;
        try
        {
            inputStream = req.getInputStream();
            FileEntry put = fileSystem.put( parent, name, inputStream );
            if ( put != null )
            {
                resp.setStatus( 200 );
                return;
            }
        }
        finally
        {
            IOUtils.closeQuietly( inputStream );
        }
        resp.sendError( 405 );
    }

    protected void doDelete( HttpServletRequest req, HttpServletResponse resp )
        throws ServletException, IOException
    {
        String path = req.getPathInfo();
        String context;
        if ( path == null )
        {
            path = req.getServletPath();
            context = req.getContextPath();
        }
        else
        {
            context = req.getContextPath() + req.getServletPath();
        }
        Entry entry = fileSystem.get( path );
        if ( entry == null )
        {
            resp.setStatus( 200 );
            return;
        }
        try
        {
            fileSystem.remove( entry );
            resp.setStatus( 200 );
        }
        catch ( UnsupportedOperationException e )
        {
            resp.sendError( 405 );
        }
    }

    private static String formatName( String name )
    {
        if ( name.length() < 50 )
        {
            return name;
        }
        return name.substring( 0, 49 ) + ">";
    }

}
