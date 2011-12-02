package org.codehaus.mojo.mrm.plugin;

import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.DiskArtifactStore;

import java.io.File;

/**
 * A locally stored Maven repository.
 *
 * @since 1.0
 */
public class LocalRepo
    implements ArtifactStoreFactory
{

    /**
     * Our source.
     *
     * @since 1.0
     */
    private File source;

    /**
     * {@inheritDoc}
     */
    public ArtifactStore newInstance()
    {
        if ( source == null )
        {
            throw new IllegalStateException( "Must provide the 'source' of the local repository" );
        }
        return new DiskArtifactStore( source );
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append( "Locally hosted (source: " ).append( source );
        sb.append( ')' );
        return sb.toString();
    }
}
