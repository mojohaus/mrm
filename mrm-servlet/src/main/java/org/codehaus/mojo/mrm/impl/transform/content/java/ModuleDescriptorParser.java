package org.codehaus.mojo.mrm.impl.transform.content.java;

import java.io.InputStream;
import java.lang.module.ModuleDescriptor;

public interface ModuleDescriptorParser {

    ModuleDescriptor parse(InputStream input) throws ParseException;
}
