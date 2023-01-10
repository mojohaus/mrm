package org.codehaus.mojo.mrm.plugin;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.DiskArtifactStore;

/**
 * A locally stored Maven repository.
 *
 * @since 1.0
 */
@Named("localRepo")
@Singleton
public class LocalRepo implements ArtifactStoreFactory {

    /**
     * Our source.
     *
     * @since 1.0
     */
    private File source;

    @Override
    public ArtifactStore newInstance(MavenSession session, Log log) {
        if (source == null) {
            throw new IllegalStateException("Must provide the 'source' of the local repository");
        }
        return new DiskArtifactStore(source);
    }

    @Override
    public void setFactoryHelper(FactoryHelper ignored) {}

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Locally hosted (source: " + source + ')';
    }
}
