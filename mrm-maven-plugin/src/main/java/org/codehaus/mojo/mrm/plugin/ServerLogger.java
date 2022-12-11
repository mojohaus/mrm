package org.codehaus.mojo.mrm.plugin;

/*
 * Copyright MojoHaus and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.log.Slf4jLog;

/**
 * Jetty SLF4J logger with debug level configuration.
 *
 * @author Slawomir Jaranowski
 */
class ServerLogger extends Slf4jLog {
    private final boolean debugEnabled;

    ServerLogger(boolean debugEnabled) {
        this(ServerLogger.class.getName(), debugEnabled);
    }

    ServerLogger(String name, boolean debugEnabled) {
        super(name);
        this.debugEnabled = debugEnabled;
    }

    @Override
    public void debug(String msg, Object... args) {
        if (isDebugEnabled()) {
            super.debug(msg, args);
        }
    }

    @Override
    public void debug(String msg, long arg) {
        if (isDebugEnabled()) {
            super.debug(msg, arg);
        }
    }

    @Override
    public void debug(Throwable thrown) {
        if (isDebugEnabled()) {
            super.debug(thrown);
        }
    }

    @Override
    public void debug(String msg, Throwable thrown) {
        if (isDebugEnabled()) {
            super.debug(msg, thrown);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    @Override
    public void setDebugEnabled(boolean enabled) {
        // do nothing
    }

    @Override
    protected Logger newLogger(String fullname) {
        return new ServerLogger(fullname, debugEnabled);
    }
}
