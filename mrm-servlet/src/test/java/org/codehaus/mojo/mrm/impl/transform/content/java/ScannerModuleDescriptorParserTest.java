package org.codehaus.mojo.mrm.impl.transform.content.java;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScannerModuleDescriptorParserTest {

    private ScannerModuleDescriptorParser parser;

    @BeforeEach
    void setup() {
        parser = new ScannerModuleDescriptorParser();
    }

    @Test
    void closedModule() throws Exception {
        String content = """
        		module mrm.test.lib.closed {}
    			""";

        ModuleDescriptor descriptor = parse(content);
        assertEquals("mrm.test.lib.closed", descriptor.name());
        assertFalse(descriptor.isOpen());
        assertFalse(descriptor.isAutomatic());
    }

    @Test
    void openModule() throws Exception {
        String content = """
        		open module mrm.test.lib.open {}
    			""";

        ModuleDescriptor descriptor = parse(content);
        assertEquals("mrm.test.lib.open", descriptor.name());
        assertTrue(descriptor.isOpen());
        assertFalse(descriptor.isAutomatic());
    }

    @Test
    void requirements() throws Exception {
        String content = """
        		module mrm.test.lib {
        		  requires com.foo.bar;
        		  requires static com.foo.baz;
        		  requires transitive com.foo.bax;
        		  requires static transitive com.foo.bay;
        		}
    			""";

        ModuleDescriptor descriptor = parse(content);
        Map<String, Requires> reqMap = descriptor.requires().stream().collect(Collectors.toMap(r -> r.name(), r -> r));

        Requires req1 = reqMap.get("com.foo.bar");
        assertNotNull(req1);
        assertEquals(Set.of(), req1.modifiers());
        Requires req2 = reqMap.get("com.foo.baz");
        assertNotNull(req2);
        assertEquals(Set.of(ModuleDescriptor.Requires.Modifier.STATIC), req2.modifiers());
        Requires req3 = reqMap.get("com.foo.bax");
        assertNotNull(req3);
        assertEquals(Set.of(ModuleDescriptor.Requires.Modifier.TRANSITIVE), req3.modifiers());
        Requires req4 = reqMap.get("com.foo.bay");
        assertNotNull(req4);
        assertEquals(
                Set.of(ModuleDescriptor.Requires.Modifier.TRANSITIVE, ModuleDescriptor.Requires.Modifier.STATIC),
                req4.modifiers());
    }

    private ModuleDescriptor parse(String content) throws IOException {
        try (InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            return parser.parse(in);
        }
    }
}
