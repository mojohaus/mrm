package org.codehaus.mojo.mrm.impl.transform.content;

import java.util.Map;

import org.codehaus.mojo.mrm.impl.transform.content.java.JavaFileTransformer;

public class InputStreamTransformerLocator {

    private final Map<String, InputStreamTransformer> transformers;

    public InputStreamTransformerLocator() {
        // On request for pluggable transformers, this could be done with CDI and/or SPI
        this.transformers = Map.of("javaToClass", new JavaFileTransformer());
    }

    public InputStreamTransformer lookup(String name) {
        InputStreamTransformer transformer = transformers.get(name);

        if (transformer == null) {
            throw new IllegalArgumentException(name + " not registered as a transformer");
        } else {
            return transformer;
        }
    }
}
