package org.codehaus.mojo.mrm.impl.transform.content;

import java.util.Map;
import java.util.NoSuchElementException;

import org.codehaus.mojo.mrm.impl.transform.content.java.JavaFileTransformer;

/**
 * Locator of InputStreamTransformer, which is currently a static list, but might be turned into CDI and/or SPI based lookup
 *
 * @since 2.0.0
 */
public class InputStreamTransformerLocator {

    private final Map<String, InputStreamTransformer> transformers;

    /**
     * Default constructor
     */
    public InputStreamTransformerLocator() {
        // On request for pluggable transformers, this could be done with CDI and/or SPI
        this.transformers = Map.of("javaToClass", new JavaFileTransformer());
    }

    /**
     * Try to lookup the transformer
     *
     * @param name the name of the transformer
     * @return the located transformer
     * @throws NoSuchElementException if no transformer is available with this name
     */
    public InputStreamTransformer lookup(String name) {
        InputStreamTransformer transformer = transformers.get(name);

        if (transformer == null) {
            throw new NoSuchElementException(name + " not registered as a transformer");
        } else {
            return transformer;
        }
    }
}
