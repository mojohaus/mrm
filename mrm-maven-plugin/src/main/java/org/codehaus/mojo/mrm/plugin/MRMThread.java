package org.codehaus.mojo.mrm.plugin;

import org.codehaus.mojo.mrm.api.FileSystem;
import org.codehaus.mojo.mrm.servlet.FileSystemServlet;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: stephenc
 * Date: 09/11/2011
 * Time: 10:02
 * To change this template use File | Settings | File Templates.
 */
public class MRMThread
    extends Thread
{
    private final Object lock = new Object();

    private boolean started = false;

    private boolean finished = false;

    private int boundPort = 0;

    private Exception problem = null;

    private final FileSystem fileSystem;

    private final int requestedPort;

    public MRMThread( String name, int port, FileSystem fileSystem )
    {
        this.fileSystem = fileSystem;
        this.requestedPort = port;
        setName( "Mock-Repository-Manager[" + name + "]" );
        setDaemon( true );
    }

    public void ensureStarted()
        throws Exception
    {
        synchronized ( lock )
        {
            if ( started )
            {
                return;
            }
            finished = false;
        }
        start();
        synchronized ( lock )
        {
            while ( !started && !finished )
            {
                lock.wait();
            }
            if ( problem != null )
            {
                throw problem;
            }
        }
    }

    public void run()
    {
        try
        {
            Server server = new Server(requestedPort);
            try
            {
                Context root = new Context( server, "/", Context.SESSIONS );
                root.addServlet( new ServletHolder( new FileSystemServlet(fileSystem) ), "/*" );
                server.start();
                synchronized ( lock )
                {
                    boundPort = 0;
                    Connector[] connectors = server.getConnectors();
                    for (int i = 0; i < connectors.length; i++) {
                        if (connectors[i].getLocalPort() > 0) {
                            boundPort = connectors[i].getLocalPort();
                            break;
                        }
                    }
                    started = true;
                    lock.notifyAll();
                }
            }
            catch ( IOException e )
            {
                problem = e;
                throw e;
            }
            catch ( InterruptedException e )
            {
                problem = e;
                throw e;
            }
            catch ( Exception e )
            {
                problem = e;
                throw e;
            }
            while ( !isFinished() )
            {
                getSomeSleep();
            }
            server.stop();
            server.join();
        }
        catch ( IOException e )
        {
            // ignore
        }
        catch ( InterruptedException e )
        {
            // ignore
        }
        catch ( Exception e )
        {
            // ignore
        }
        finally
        {
            finish();
        }
    }

    public boolean isFinished()
    {
        synchronized ( lock )
        {
            return finished;
        }
    }

    private void getSomeSleep()
    {
        synchronized ( lock )
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

    public void finish()
    {
        synchronized ( lock )
        {
            finished = true;
            started = false;
            lock.notifyAll();
        }
    }

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


    public int getPort()
    {
        synchronized ( lock ) {
        return started ? boundPort : requestedPort;
        }
    }

    String getUrl()
    {
        return "http://localhost:" + getPort() + "/";
    }
}
