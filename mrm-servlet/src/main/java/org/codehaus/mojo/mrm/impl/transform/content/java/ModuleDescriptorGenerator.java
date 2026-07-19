package org.codehaus.mojo.mrm.impl.transform.content.java;

import java.io.InputStream;
import java.lang.module.ModuleDescriptor;

interface ModuleDescriptorGenerator {

    InputStream generate(ModuleDescriptor descriptor);
}
