package org.codehaus.mojo.mrm.impl.transform.content.java;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        assertEquals(in, out);
    }

    @Test
    void openModule() throws Exception {
        ModuleDescriptor in = ModuleDescriptor.newModule("mrm.test.lib.open", Set.of(ModuleDescriptor.Modifier.OPEN))
                .build();

        ModuleDescriptor out = ModuleDescriptor.read(generator.generate(in));

        assertEquals(in, out);
    }

    @Test
    void requirements() throws Exception {
        ModuleDescriptor in = ModuleDescriptor.newModule("mrm.test.lib")
                .requires("com.foo.bar")
                .requires(Set.of(Requires.Modifier.STATIC), "com.foo.baz")
                .requires(Set.of(Requires.Modifier.TRANSITIVE), "com.foo.bax")
                .build();

        // not testing here with 2 modifiers as this will result in a flaky test.
        // most likely cause: java.lang.module.ModuleDescriptor.modsHashCode()
        // this ignores that order of Sets is irrelevant for equals.

        ModuleDescriptor out = ModuleDescriptor.read(generator.generate(in));

        assertEquals(out, in);
    }
}
