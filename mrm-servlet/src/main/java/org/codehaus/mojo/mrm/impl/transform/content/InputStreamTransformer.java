package org.codehaus.mojo.mrm.impl.transform.content;

import java.io.InputStream;

/**
 * Functional interface to use when transforming inputStreams
 *
 * @since 2.0.0
 */
@FunctionalInterface
public interface InputStreamTransformer {

    /**
     * Transformation result based on the inputStream and filename
     *
     * @param inputStream the input
     * @param filename the original filename
     * @return the transformed input
     */
    InputStream transform(InputStream inputStream, String filename);
}
