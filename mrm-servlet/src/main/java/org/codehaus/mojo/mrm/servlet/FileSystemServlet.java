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

package org.codehaus.mojo.mrm.servlet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.mojo.mrm.api.DefaultDirectoryEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.Entry;
import org.codehaus.mojo.mrm.api.FileEntry;
import org.codehaus.mojo.mrm.api.FileSystem;
import org.codehaus.mojo.mrm.impl.MemoryFileSystem;
import org.codehaus.mojo.mrm.impl.Utils;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileSystemServlet
    extends HttpServlet
{

    public FileSystemServlet()
    {
        this.fileSystem = new MemoryFileSystem();
    }

    public FileSystemServlet( FileSystem fileSystem )
    {
        this.fileSystem = fileSystem;
    }

    private FileSystem fileSystem;

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
