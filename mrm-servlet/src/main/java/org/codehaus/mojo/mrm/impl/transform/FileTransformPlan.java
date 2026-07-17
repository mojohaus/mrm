package org.codehaus.mojo.mrm.impl.transform;

import java.io.InputStream;
import java.util.function.UnaryOperator;

public interface FileTransformPlan {

    default UnaryOperator<String> filename() {
        return UnaryOperator.identity();
    }

    default UnaryOperator<InputStream> inputStream(String fileName) {
        return UnaryOperator.identity();
    }
}
