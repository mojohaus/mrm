package org.codehaus.mojo.mrm.plugin;

import java.io.File;

import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.DiskArtifactStore;

/**
 * Repository used for distribution management
 * 
 * @author Robert Scholte
 * @since 1.1.0
 */
public class HostedRepo implements ArtifactStoreFactory
{
    /**
     * The directory to store the uploaded files
     */
    private File target;
    
    @Override
    public ArtifactStore newInstance()
    {
        if ( target == null )
        {
            throw new IllegalStateException( "Must provide the 'target' of the hosted repository" );
        }
        return new DiskArtifactStore( target ).canWrite( true );
    }

    @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append( "Remote hosted (target: " ).append( target );
        sb.append( ')' );
        return sb.toString();
    }
}
