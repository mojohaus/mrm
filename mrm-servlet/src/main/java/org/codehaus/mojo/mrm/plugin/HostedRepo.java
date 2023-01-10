package org.codehaus.mojo.mrm.plugin;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.DiskArtifactStore;

/**
 * Repository used for distribution management
 *
 * @author Robert Scholte
 * @since 1.1.0
 */
@Named("hostedRepo")
@Singleton
public class HostedRepo implements ArtifactStoreFactory {
    /**
     * The directory to store the uploaded files
     */
    private File target;

    @Override
    public ArtifactStore newInstance(MavenSession session, Log log) {
        if (target == null) {
            throw new IllegalStateException("Must provide the 'target' of the hosted repository");
        }
        return new DiskArtifactStore(target).canWrite(true);
    }

    @Override
    public void setFactoryHelper(FactoryHelper ignored) {}

    @Override
    public String toString() {
        return "Remote hosted (target: " + target + ')';
    }
}
