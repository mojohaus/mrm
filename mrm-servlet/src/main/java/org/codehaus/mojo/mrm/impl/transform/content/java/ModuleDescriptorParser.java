package org.codehaus.mojo.mrm.impl.transform.content.java;

import java.io.InputStream;
import java.lang.module.ModuleDescriptor;

/**
 * Interface to implement when providing code to parse a module descriptor source into its model
 *
 * @since 2.0.0
 */
public interface ModuleDescriptorParser {

    /**
     * Parse the source into a ModuleDescriptor
     *
     * @param input the module descriptor as source
     * @return the module descriptor
     * @throws ParseException when parsing fails
     */
    ModuleDescriptor parse(InputStream input) throws ParseException;
}
