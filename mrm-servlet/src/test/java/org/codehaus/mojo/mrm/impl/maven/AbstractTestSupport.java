package org.codehaus.mojo.mrm.impl.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Optional;

abstract class AbstractTestSupport {

    protected File getResourceAsFile(String path) throws FileNotFoundException {
        try {
            return new File(Optional.ofNullable(getClass().getResource(path))
                    .orElseThrow(() -> new FileNotFoundException(path))
                    .toURI());
        } catch (URISyntaxException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }
}
