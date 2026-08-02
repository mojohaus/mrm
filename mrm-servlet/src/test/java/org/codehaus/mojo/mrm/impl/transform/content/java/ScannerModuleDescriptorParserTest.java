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

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(descriptor.name()).isEqualTo("mrm.test.lib.closed");
        assertThat(descriptor.isOpen()).isFalse();
        assertThat(descriptor.isAutomatic()).isFalse();
    }

    @Test
    void openModule() throws Exception {
        String content = """
        		open module mrm.test.lib.open {}
    			""";

        ModuleDescriptor descriptor = parse(content);
        assertThat(descriptor.name()).isEqualTo("mrm.test.lib.open");
        assertThat(descriptor.isOpen()).isTrue();
        assertThat(descriptor.isAutomatic()).isFalse();
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
        assertThat(req1).isNotNull();
        assertThat(req1.modifiers()).isEqualTo(Set.of());
        Requires req2 = reqMap.get("com.foo.baz");
        assertThat(req2).isNotNull();
        assertThat(req2.modifiers()).isEqualTo(Set.of(ModuleDescriptor.Requires.Modifier.STATIC));
        Requires req3 = reqMap.get("com.foo.bax");
        assertThat(req3).isNotNull();
        assertThat(req3.modifiers()).isEqualTo(Set.of(ModuleDescriptor.Requires.Modifier.TRANSITIVE));
        Requires req4 = reqMap.get("com.foo.bay");
        assertThat(req4).isNotNull();
        assertThat(req4.modifiers())
                .isEqualTo(Set.of(
                        ModuleDescriptor.Requires.Modifier.TRANSITIVE, ModuleDescriptor.Requires.Modifier.STATIC));
    }

    private ModuleDescriptor parse(String content) throws IOException {
        try (InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            return parser.parse(in);
        }
    }
}
