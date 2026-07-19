package org.codehaus.mojo.mrm.impl.transform.content.java;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.util.Collection;
import java.util.stream.Collectors;

import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.attribute.ModuleAttribute;
import io.smallrye.classfile.extras.constant.ModuleDesc;
import io.smallrye.classfile.extras.reflect.AccessFlag;

/**
 * Transforms a ModuleDescriptor to its binary form using the smallrye backport of the classfile-api
 *
 * @since 2.0.0
 */
final class ClassFileApiModuleDescriptorGenerator implements ModuleDescriptorGenerator {

    @Override
    public InputStream generate(ModuleDescriptor descriptor) {
        ModuleAttribute moduleAttribute = ModuleAttribute.of(ModuleDesc.of(descriptor.name()), moduleBuilder -> {
            if (descriptor.isOpen()) {
                moduleBuilder.moduleFlags(AccessFlag.OPEN);
            }

            for (ModuleDescriptor.Requires req : descriptor.requires()) {
                Collection<AccessFlag> modifiers = req.modifiers().stream()
                        .map(ClassFileApiModuleDescriptorGenerator::toAccessFlag)
                        .collect(Collectors.toSet());

                moduleBuilder.requires(ModuleDesc.of(req.name()), modifiers, null);
            }
        });

        // As long as we use the backport, we can't use ClassFile.latestMajorVersion()
        String classVersionProperty = System.getProperty("java.class.version");

        int activeRuntimeMajor = (int) Double.parseDouble(classVersionProperty);

        byte[] classBytes = ClassFile.of().buildModule(moduleAttribute, cb -> cb.withVersion(activeRuntimeMajor, 0));

        return new ByteArrayInputStream(classBytes);
    }

    private static AccessFlag toAccessFlag(Requires.Modifier modifier) {
        return switch (modifier) {
            case MANDATED -> AccessFlag.MANDATED;
            case STATIC -> AccessFlag.STATIC_PHASE;
            case SYNTHETIC -> AccessFlag.SYNTHETIC;
            case TRANSITIVE -> AccessFlag.TRANSITIVE;
        };
    }
}
