package org.codehaus.mojo.mrm.plugin;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.mrm.api.FileSystem;
import org.codehaus.mojo.mrm.servlet.FileSystemServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A file system server.
 */
public class FileSystemServer
{

    /**
     * Guard for {@link #starting}, {@link #started}, {@link #finishing}, {@link #finished}, {@link #boundPort}
     * and {@link #problem}.
     */
    private final Object lock = new Object();

    /**
     * Flag to indicate that the thread is starting up.
     * <p/>
     * Guarded by {@link #lock}.
     */
    private boolean starting = false;

    /**
     * Flag to indicate that the thread is ready to serve requests.
     * <p/>
     * Guarded by {@link #lock}.
     */
    private boolean started = false;

    /**
     * Flag to indicate that the thread is terminated.
     * <p/>
     * Guarded by {@link #lock}.
     */
    private boolean finished = false;

    /**
     * Flag to indicate that the thread should terminate.
     * <p/>
     * Guarded by {@link #lock}.
     */
    private boolean finishing = false;

    /**
     * The port that the server is bound to.
     * <p/>
     * Guarded by {@link #lock}.
     */
    private int boundPort = 0;

    /**
     * The port that the server is bound to.
     * <p/>
     * Guarded by {@link #lock}.
     */
    private Exception problem = null;

    /**
     * The name of the file system (used to name the thread).
     */
    private final String name;

    /**
     * The file system to serve.
     */
    private final FileSystem fileSystem;

    /**
     * The port to try and serve on.
     */
    private final int requestedPort;

    /**
     * The context path where to bind the {@code fileSystem}.
     */
    private final String contextPath;

    /**
     * The path to settingsFile containing the configuration to connect to this repository manager.
     */
    private final String settingsServletPath;

    /**
     * Indicate debug level by Jetty server
     */
    private final boolean debugServer;

    /**
     * Creates a new file system server that will serve a {@link FileSystem} over HTTP on the specified port.
     *
     * @param name        The name of the file system server thread.
     * @param port        The port to server on or <code>0</code> to pick a random, but available, port.
     * @param fileSystem  the file system to serve.
     * @param debugServer the server debug mode
     */
    public FileSystemServer( String name, int port, String contextPath, FileSystem fileSystem,
                             String settingsServletPath, boolean debugServer )
    {
        this.name = name;
        this.fileSystem = fileSystem;
        this.requestedPort = port;
        this.contextPath = sanitizeContextPath( contextPath );
        this.settingsServletPath = settingsServletPath;
        this.debugServer = debugServer;
    }

    /**
     * Sanitize the given {@code contextPath} by prepending slash if necessary and/or removing the trailing slash if
     * necessary
     *
     * @param contextPath the contextPath to sanitize
     * @return sanitized {@code contextPath}
     */
    static String sanitizeContextPath( String contextPath )
    {
        if ( contextPath == null || contextPath.isEmpty() || contextPath.equals( "/" ) )
        {
            return "/";
        }
        if ( !contextPath.startsWith( "/" ) )
        {
            return "/" + contextPath;
        }
        if ( contextPath.endsWith( "/" ) )
        {
            return contextPath.substring( 0, contextPath.length() - 1 );
        }
        return contextPath;
    }

    /**
     * Ensures that the file system server is started (if already starting, will block until started, otherwise starts
     * the file system server and blocks until started)
     *
     * @throws MojoExecutionException if the file system server could not be started.
     */
    public void ensureStarted()
        throws MojoExecutionException
    {
        synchronized ( lock )
        {
            if ( started || starting )
            {
                return;
            }
            starting = true;
            started = false;
            finished = false;
            finishing = false;
        }
        Thread worker = new Thread( new Worker(), "FileSystemServer[" + name + "]" );
        worker.setDaemon( true );
        worker.start();
        try
        {
            synchronized ( lock )
            {
                while ( starting && !started && !finished && !finishing )
                {
                    lock.wait();
                }
                if ( problem != null )
                {
                    throw problem;
                }
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }


    /**
     * Returns <code>true</code> if and only if the file system server is finished.
     *
     * @return <code>true</code> if and only if the file system server is finished.
     */
    public boolean isFinished()
    {
        synchronized ( lock )
        {
            return finished;
        }
    }

    /**
     * Returns <code>true</code> if and only if the file system server is started.
     *
     * @return <code>true</code> if and only if the file system server is started.
     */
    public boolean isStarted()
    {
        synchronized ( lock )
        {
            return finished;
        }
    }

    /**
     * Signal the file system server to shut down.
     */
    public void finish()
    {
        synchronized ( lock )
        {
            finishing = true;
            lock.notifyAll();
        }
    }

    /**
     * Blocks until the file system server has actually shut down.
     *
     * @throws InterruptedException if interrupted.
     */
    public void waitForFinished()
        throws InterruptedException
    {
        synchronized ( lock )
        {
            while ( !finished )
            {
                lock.wait();
            }
        }
    }

    /**
     * Gets the port that the file system server is/will server on.
     *
     * @return the port that the file system server is/will server on.
     */
    public int getPort()
    {
        synchronized ( lock )
        {
            return started ? boundPort : requestedPort;
        }
    }

    /**
     * Gets the root url that the file system server is/will server on.
     *
     * @return the root url that the file system server is/will server on.
     */
    public String getUrl()
    {
        return "http://localhost:" + getPort() + ( contextPath.equals( "/" ) ? "" : contextPath );
    }

    /**
     * Same as {@link #getUrl()}, but now for remote users
     *
     * @return the scheme + raw IP address + port + contextPath
     * @throws UnknownHostException if the local host name could not be resolved into an address.
     */
    public String getRemoteUrl() throws UnknownHostException
    {
        return "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + getPort()
            + ( contextPath.equals( "/" ) ? "" : contextPath );
    }

    /**
     * The work to monitor and control the Jetty instance that hosts the file system.
     */
    private final class Worker
        implements Runnable
    {
        /**
         * {@inheritDoc}
         */
        public void run()
        {
            try
            {
                Logger serverLogger = new ServerLogger( debugServer );
                Log.setLog( serverLogger );
                Log.initialized();

                Server server = new Server( requestedPort );

                try
                {
                    ServletContextHandler context = new ServletContextHandler();
                    context.setContextPath( contextPath );
                    context.addServlet( new ServletHolder( new FileSystemServlet( fileSystem, settingsServletPath ) ),
                                        "/*" );
                    server.setHandler( context );
                    server.start();
                    synchronized ( lock )
                    {
                        boundPort = ( (ServerConnector) server.getConnectors()[0] ).getLocalPort();
                        starting = false;
                        started = true;
                        lock.notifyAll();
                    }
                }
                catch ( Exception e )
                {
                    synchronized ( lock )
                    {
                        problem = e;
                    }
                    serverLogger.warn( e );
                    throw e;
                }
                synchronized ( lock )
                {
                    while ( !finishing )
                    {
                        try
                        {
                            lock.wait( 500 );
                        }
                        catch ( InterruptedException e )
                        {
                            // ignore
                        }
                    }
                }
                server.stop();
                server.join();
            }
            catch ( Exception e )
            {
                // ignore
            }
            finally
            {
                synchronized ( lock )
                {
                    started = false;
                    starting = false;
                    finishing = false;
                    finished = true;
                    boundPort = 0;
                    lock.notifyAll();
                }
            }
        }
    }

}
