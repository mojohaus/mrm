package org.codehaus.mojo.mrm.impl.transform;

import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.function.UnaryOperator;

/**
 * A FileTransformPlan provides all methods to the transformations.
 * All transformations start with calling the transformation result for a filename.
 * All other methods use the input filename as context to be able to do their transformation.
 *
 * @since 2.0.0
 */
public sealed interface FileTransformPlan permits AbstractFileTransformPlan {

    /**
     *
     * @return the filename transformation instruction, by default no transformation
     */
    UnaryOperator<String> filename();

    /**
     * Every call of {@link #filename()} is registered, this will give its input based on its output
     *
     * @param fileName the context
     * @return the original filename
     * @throws NoSuchElementException if there is no input known for this output
     */
    String originalFilename(String fileName);

    /**
     *
     * @param fileName the context
     * @return the content transformation instruction, by default no transformation
     */
    default UnaryOperator<InputStream> content(String fileName) {
        return UnaryOperator.identity();
    }
}
