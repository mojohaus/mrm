package org.codehaus.mojo.mrm.plugin;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.MockArtifactStore;
import org.codehaus.mojo.mrm.impl.transform.TransformDirectiveSourceFactory;
import org.codehaus.mojo.mrm.impl.transform.metadata.MetadataTransformDirective;
import org.codehaus.mojo.mrm.impl.transform.metadata.MetadataTransformDirectiveFactory;

/**
 * A mock Maven repository.
 *
 * @since 1.0
 */
public class MockRepo implements ArtifactStoreFactory {

    /**
     * Our source.
     *
     * @since 1.0
     */
    private File source;

    /**
     * Clone the {@link #source} to a specific directory.
     * Set this when using directory based archives.
     *
     * @since 1.1.0
     */
    private File cloneTo;

    /**
     * Ensure that the {@link #cloneTo} folder is clean before every run.
     *
     * @since 1.1.0
     */
    private boolean cloneClean;

    /**
     * Set to {@code false} if directories should archived at startup, or to {@code true} just when used.
     *
     * @since 1.1.0
     */
    private boolean lazyArchiver;

    /**
     * Define where the transformation directives are coming from. Supported values: metadata.
     *
     * @since 2.0.0
     * @see MetadataTransformDirective
     */
    private String transformDirectiveSource;

    @Override
    public ArtifactStore newInstance(FactoryHelper factoryHelper) {
        if (source == null) {
            throw new IllegalStateException("Must provide the 'source' of the mock repository");
        }

        File root = source;
        if (cloneTo != null) {
            if (!cloneTo.mkdirs() && cloneClean) {
                try {
                    FileUtils.cleanDirectory(cloneTo);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to clean directory: " + e.getMessage());
                }
            }

            try {
                FileUtils.copyDirectory(source, cloneTo);
                root = cloneTo;
            } catch (IOException e) {
                throw new IllegalStateException("Failed to copy directory: " + e.getMessage());
            }
        }

        TransformDirectiveSourceFactory tds;
        if (transformDirectiveSource == null) {
            tds = null;
        } else {
            tds = switch (transformDirectiveSource) {
                case "metadata" -> new MetadataTransformDirectiveFactory();
                default ->
                    throw new IllegalArgumentException("Unknown transformDirectiveSource: " + transformDirectiveSource);
            };
        }

        return new MockArtifactStore(factoryHelper.getArchiverManager(), root, lazyArchiver, tds);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Mock content (source: " + source + ')';
    }
}
