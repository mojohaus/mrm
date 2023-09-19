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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.impl.digest.AutoDigestFileSystem;
import org.codehaus.mojo.mrm.impl.maven.ArtifactStoreFileSystem;
import org.codehaus.mojo.mrm.impl.maven.CompositeArtifactStore;

/**
 * Common base class for the mojos that start a repository.
 *
 * @since 1.0
 */
public abstract class AbstractStartMojo extends AbstractMRMMojo {

    private final FactoryHelper factoryHelper;

    /**
     * The port to serve the repository on. If not specified a random port will be used.
     */
    @Parameter(property = "mrm.port")
    private int port;

    /**
     * The base path under which the repository will be served.
     * <p>
     * By default, {@code org.acme:my-artifact:pom:1.2.3} will be served under
     * {@code http://localhost:<port>/org/acme/my-artifact/1.2.3/my-artifact-1.2.3.pom}.
     * <p>
     * If {@code basePath} is set to e.g. {@code foo/bar} then {@code org.acme:my-artifact:pom:1.2.3} will be served
     * under {@code http://localhost:<port>/foo/bar/org/acme/my-artifact/1.2.3/my-artifact-1.2.3.pom}.
     *
     * @since 1.4.0
     */
    @Parameter(property = "mrm.basePath", defaultValue = "/")
    private String basePath;

    /**
     * The repositories to serve. When more than one repository is specified, a merged repository view
     * of those will be used. If none specified then a proxy of the invoking Maven's repositories will
     * be served.
     */
    @Parameter
    private ArtifactStoreFactory[] repositories;

    /**
     * Indicate if Jetty server should produce logs in debug level.
     * <p>
     * <b>Notice:</b> It is taken into account only when Maven is started in verbose mode.
     *
     * @since 1.5.0
     */
    @Parameter(property = "mrm.debugServer", defaultValue = "false")
    private boolean debugServer;

    /**
     * Creates a new instance
     * @param factoryHelper injected {@link FactoryHelper} instance
     * @param proxyRepo injected proxyHelper instance
     */
    public AbstractStartMojo(FactoryHelper factoryHelper, ArtifactStoreFactory proxyRepo) {
        super(proxyRepo);
        this.factoryHelper = factoryHelper;
    }

    /**
     * Creates a file system server from an artifact store.
     *
     * @param artifactStore the artifact store to serve.
     * @return the file system server.
     */
    protected FileSystemServer createFileSystemServer(ArtifactStore artifactStore) {
        return new FileSystemServer(
                ArtifactUtils.versionlessKey(project.getGroupId(), project.getArtifactId()),
                Math.max(0, Math.min(port, 65535)),
                basePath,
                new AutoDigestFileSystem(new ArtifactStoreFileSystem(artifactStore)),
                debugServer);
    }

    /**
     * Creates an artifact store from the {@link #repositories} configuration.
     *
     * @return an artifact store.
     * @throws org.apache.maven.plugin.MojoExecutionException if the configuration is invalid.
     */
    protected ArtifactStore createArtifactStore() throws MojoExecutionException {
        Objects.requireNonNull(proxyRepo);
        if (repositories == null) {
            getLog().info("Configuring Mock Repository Manager to proxy through this Maven instance");
            return createProxyArtifactStore();
        }
        getLog().info("Configuring Mock Repository Manager...");
        List<ArtifactStore> stores = new ArrayList<>();
        if (repositories == null || repositories.length == 0) {
            repositories = new ArtifactStoreFactory[] {proxyRepo};
        }
        for (ArtifactStoreFactory artifactStoreFactory : repositories) {
            getLog().info("  " + artifactStoreFactory.toString());
            if (artifactStoreFactory != proxyRepo) {
                artifactStoreFactory.setFactoryHelper(factoryHelper);
            }
            stores.add(artifactStoreFactory.newInstance(session, getLog()));
        }
        ArtifactStore[] artifactStores = stores.toArray(new ArtifactStore[0]);
        return artifactStores.length == 1 ? artifactStores[0] : new CompositeArtifactStore(artifactStores);
    }
}
