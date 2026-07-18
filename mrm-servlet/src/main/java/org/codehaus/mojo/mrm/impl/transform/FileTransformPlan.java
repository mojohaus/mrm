package org.codehaus.mojo.mrm.impl.transform;

import java.io.InputStream;
import java.util.function.UnaryOperator;

/**
 * A FileTransformPlan provides all methods to the transformations.
 * All transformations start with calling the transformation result for a filename.
 * All other methods use a filename as context to be able to do their transformation.
 * The implementation is responsible for choosing the old filename or new filename.
 * e.g The Plexus Archiver uses FileSets with InputStreamTransformer, which is based on the new filename.
 *
 * @since 2.0.0
 */
public interface FileTransformPlan {

    /**
     *
     * @return the filename transformation instruction, by default no transformation
     */
    default UnaryOperator<String> filename() {
        return UnaryOperator.identity();
    }

    /**
     *
     * @param fileName the context
     * @return the content transformation instruction, by default no transformation
     */
    default UnaryOperator<InputStream> content(String fileName) {
        return UnaryOperator.identity();
    }
}
