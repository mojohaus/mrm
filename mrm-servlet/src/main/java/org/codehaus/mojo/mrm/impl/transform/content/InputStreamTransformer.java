package org.codehaus.mojo.mrm.impl.transform.content;

import java.io.InputStream;

@FunctionalInterface
public interface InputStreamTransformer {

    InputStream transform(InputStream inputStream, String filename);
}
