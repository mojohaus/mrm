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

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.impl.digest.AutoDigestFileSystem;
import org.codehaus.mojo.mrm.impl.maven.ArtifactStoreFileSystem;
import org.codehaus.mojo.mrm.impl.maven.CompositeArtifactStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Common base class for the mojos that start a repository.
 *
 * @since 1.0
 */
public abstract class AbstractStartMojo
    extends AbstractMRMMojo
{
    /**
     * The port to serve the repository on. If not specified a random port will be used.
     */
    @Parameter( property = "mrm.port" )
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
    @Parameter( property = "mrm.basePath", defaultValue = "/" )
    private String basePath;

    /**
     * The repositories to serve. When more than one repository is specified, a merged repository view
     * of those will be used. If none specified then a proxy of the invoking Maven's repositories will
     * be served.
     */
    @Parameter
    private ArtifactStoreFactory[] repositories;

    /**
     * Creates a file system server from an artifact store.
     *
     * @param artifactStore the artifact store to serve.
     * @return the file system server.
     */
    protected FileSystemServer createFileSystemServer( ArtifactStore artifactStore )
    {
        return new FileSystemServer( ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() ),
                                     Math.max( 0, Math.min( port, 65535 ) ),
                                     basePath,
                                     new AutoDigestFileSystem( new ArtifactStoreFileSystem( artifactStore ) ),
                                     getSettingsServletPath() );
    }

    /**
     * When set, this points to the to the location from where the settings file can be downloaded.
     *
     * @return the servlet path to the settings file of {@code null}
     */
    protected String getSettingsServletPath()
    {
        return null;
    }

    /**
     * Creates an artifact store from the {@link #repositories} configuration.
     *
     * @return an artifact store.
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          if the configuration is invalid.
     */
    protected ArtifactStore createArtifactStore()
        throws MojoExecutionException
    {
        if ( repositories == null )
        {
            getLog().info( "Configuring Mock Repository Manager to proxy through this Maven instance" );
            return createProxyArtifactStore();
        }
        getLog().info( "Configuring Mock Repository Manager..." );
        List<ArtifactStore> stores = new ArrayList<>();
        if ( repositories == null || repositories.length == 0 )
        {
            repositories = new ArtifactStoreFactory[]{ new ProxyRepo() };
        }
        FactoryHelper helper = createFactoryHelper();
        for ( ArtifactStoreFactory artifactStoreFactory : repositories )
        {
            if ( artifactStoreFactory instanceof FactoryHelperRequired )
            {
                ( (FactoryHelperRequired) artifactStoreFactory ).setFactoryHelper( helper );
            }
            getLog().info( "  " + artifactStoreFactory.toString() );
            stores.add( artifactStoreFactory.newInstance() );
        }
        ArtifactStore[] artifactStores = stores.toArray(new ArtifactStore[0]);
        return artifactStores.length == 1 ? artifactStores[0] : new CompositeArtifactStore( artifactStores );
    }

}
