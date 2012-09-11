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

import java.io.IOException;

/**
 * A thread that waits for the user to press a key.
 */
public class ConsoleScanner
    extends Thread
{
    /**
     * The guard for {@link #finished}.
     */
    private final Object lock = new Object();

    /**
     * Flag to indicate that the thread has finished.
     * <p/>
     * Guarded by {@link #lock}.
     */
    private boolean finished = false;

    /**
     * creates a new instance.
     */
    public ConsoleScanner()
    {
        setName( "Console scanner" );
        setDaemon( true );
    }

    /**
     * {@inheritDoc}
     */
    public void run()
    {
        try
        {
            synchronized ( lock )
            {
                try
                {
                    while ( !finished )
                    {
                        checkSystemInput();
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
                finally
                {
                    synchronized ( lock )
                    {
                        finished = true;
                        lock.notifyAll();
                    }
                }
            }
        }
        catch ( IOException e )
        {
            // ignore
        }
    }

    /**
     * Checks for the user pressing Enter.
     *
     * @throws IOException if something went wrong.
     */
    private void checkSystemInput()
        throws IOException
    {
        while ( System.in.available() > 0 )
        {
            int input = System.in.read();
            if ( input >= 0 )
            {
                char c = (char) input;
                if ( c == '\n' )
                {
                    synchronized ( lock )
                    {
                        finished = true;
                        lock.notifyAll();
                    }
                }
            }
            else
            {
                synchronized ( lock )
                {
                    finished = true;
                    lock.notifyAll();
                }
            }
        }
    }

    /**
     * Blocks until the console scanner is finished.
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
}
