package org.codehaus.mojo.mrm.impl.transform.content.java;

import java.io.InputStream;
import java.lang.module.ModuleDescriptor;

/**
 * Interface to implement when providing code to generate binary code for a module descriptor
 *
 * @since 2.0.0
 */
interface ModuleDescriptorGenerator {

    InputStream generate(ModuleDescriptor descriptor);
}
