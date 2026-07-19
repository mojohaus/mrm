package org.codehaus.mojo.mrm.impl.transform;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.UnaryOperator;

public non-sealed class AbstractFileTransformPlan implements FileTransformPlan {

    private final Map<String, String> origin = new HashMap<>();

    @Override
    public final UnaryOperator<String> filename() {
        return input -> {
            var output = doFilename().apply(input);
            origin.put(output, input);
            return output;
        };
    }

    protected UnaryOperator<String> doFilename() {
        return UnaryOperator.identity();
    }

    @Override
    public final String originalFilename(String fileName) {
        String original = origin.get(fileName);
        if (original == null) {
            throw new NoSuchElementException(fileName + " not used as output");
        } else {
            return original;
        }
    }
}
