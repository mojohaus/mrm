package org.codehaus.mojo.mrm.impl.transform.content.java;

import java.io.InputStream;
import java.lang.module.ModuleDescriptor;

import org.codehaus.mojo.mrm.impl.transform.content.InputStreamTransformer;

public final class JavaFileTransformer implements InputStreamTransformer {

    private ModuleDescriptorParser moduleDescriptorParser;
    private ModuleDescriptorGenerator moduleDescriptorGenerator;

    public JavaFileTransformer() {
        // currently only 1 implementation available for both
        this.moduleDescriptorParser = new ScannerModuleDescriptorParser();
        this.moduleDescriptorGenerator = new ClassFileApiModuleDescriptorGenerator();
    }

    public String name() {
        return "javaToClass";
    }

    @Override
    public InputStream transform(InputStream inputStream, String filename) {
        if ("module-info.java".equals(filename)) {
            return transformModuleDescriptor(inputStream, filename);
        } else {
            throw new UnsupportedOperationException("Can only transform the module-info.java");
        }
    }

    public InputStream transformModuleDescriptor(InputStream inputStream, String filename) {
        try {
            ModuleDescriptor descriptor = moduleDescriptorParser.parse(inputStream);

            return moduleDescriptorGenerator.generate(descriptor);
        } catch (ParseException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
