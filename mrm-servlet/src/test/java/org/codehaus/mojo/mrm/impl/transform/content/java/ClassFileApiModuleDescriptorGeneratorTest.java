package org.codehaus.mojo.mrm.impl.transform.content.java;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassFileApiModuleDescriptorGeneratorTest {

    private ClassFileApiModuleDescriptorGenerator generator;

    @BeforeEach
    void setup() {
        generator = new ClassFileApiModuleDescriptorGenerator();
    }

    @Test
    void closedModule() throws Exception {
        ModuleDescriptor in = ModuleDescriptor.newModule("mrm.test.lib.closed").build();

        ModuleDescriptor out = ModuleDescriptor.read(generator.generate(in));

        assertThat(out).isEqualTo(in);
    }

    @Test
    void openModule() throws Exception {
        ModuleDescriptor in = ModuleDescriptor.newModule("mrm.test.lib.open", Set.of(ModuleDescriptor.Modifier.OPEN))
                .build();

        ModuleDescriptor out = ModuleDescriptor.read(generator.generate(in));

        assertThat(out).isEqualTo(in);
    }

    @Test
    void requirements() throws Exception {
        ModuleDescriptor in = ModuleDescriptor.newModule("mrm.test.lib")
                .requires("com.foo.bar")
                .requires(Set.of(Requires.Modifier.STATIC), "com.foo.baz")
                .requires(Set.of(Requires.Modifier.TRANSITIVE), "com.foo.bax")
                .build();

        // Not testing here with 2 modifiers due to https://bugs.openjdk.org/browse/JDK-8290041 (fixed with OpenJDK 20)

        ModuleDescriptor out = ModuleDescriptor.read(generator.generate(in));

        assertThat(out).isEqualTo(in);
    }
}
