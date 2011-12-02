package org.codehaus.mojo.mrm.plugin;

import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.MockArtifactStore;

import java.io.File;

/**
 * A mock Maven repository.
 *
 * @since 1.0
 */
public class MockRepo
    implements ArtifactStoreFactory, FactoryHelperRequired
{

    /**
     * Our helper.
     *
     * @since 1.0
     */
    private FactoryHelper factoryHelper;

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
        if ( factoryHelper == null )
        {
            throw new IllegalStateException( "FactoryHelper has not been set" );
        }
        if ( source == null )
        {
            throw new IllegalStateException( "Must provide the 'source' of the mock repository" );
        }
        return new MockArtifactStore( factoryHelper.getLog(), source );
    }

    /**
     * {@inheritDoc}
     */
    public void setFactoryHelper( FactoryHelper factoryHelper )
    {
        this.factoryHelper = factoryHelper;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append( "Mock content (source: " ).append( source );
        sb.append( ')' );
        return sb.toString();
    }
}
