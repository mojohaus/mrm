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

import java.net.UnknownHostException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * This goal is used in-situ on a Maven project to allow integration tests based on the Maven Invoker to use a custom
 * <code>settings.xml</code> and still work behind a proxy.
 *
 * @author Stephen Connolly
 * <p>
 * Starts a mock repository manager for manual testing.
 */
@Mojo(name = "run", requiresProject = false, requiresDirectInvocation = true, threadSafe = true)
public class RunMojo extends AbstractStartMojo {
    /**
     * ServletPath for the settings.xml, so it can be downloaded.
     */
    @Parameter(property = "mrm.settingsServletPath", defaultValue = "settings-mrm.xml")
    private String settingsServletPath;

    /**
     * {@inheritDoc}
     */
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        if (!session.getSettings().isInteractiveMode()) {
            throw new MojoExecutionException(
                    "Cannot run a mock repository in batch mode (as there is no way to signal shutdown) "
                            + "use mrm:start instead");
        }
        FileSystemServer mrm = createFileSystemServer(createArtifactStore());
        getLog().info("Starting Mock Repository Manager");
        mrm.ensureStarted();
        String url = mrm.getUrl();
        try {
            getLog().info("Mock Repository Manager " + url + " is started.");
            if (StringUtils.isNotEmpty(settingsServletPath)) {
                String downloadUrl;
                try {
                    downloadUrl = mrm.getRemoteUrl();
                } catch (UnknownHostException e) {
                    downloadUrl = mrm.getUrl();
                }

                String settings = FileUtils.filename(settingsServletPath);

                getLog().info("To share this repository manager, let users download " + downloadUrl + "/"
                        + settingsServletPath);
                getLog().info("Maven should be started as 'mvn --settings " + settings + " [phase|goal]'");
            }
            ConsoleScanner consoleScanner = new ConsoleScanner();
            consoleScanner.start();
            getLog().info("Hit ENTER on the console to stop the Mock Repository Manager and continue the build.");
            consoleScanner.waitForFinished();
        } catch (InterruptedException e) {
            // ignore
        } finally {
            getLog().info("Stopping Mock Repository Manager " + url);
            mrm.finish();
            try {
                mrm.waitForFinished();
                getLog().info("Mock Repository Manager " + url + " is stopped.");
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSettingsServletPath() {
        return settingsServletPath;
    }
}
