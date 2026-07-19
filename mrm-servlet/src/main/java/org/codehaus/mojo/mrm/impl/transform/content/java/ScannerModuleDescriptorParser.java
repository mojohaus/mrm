package org.codehaus.mojo.mrm.impl.transform.content.java;

import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A lightweight parser based on Scanner
 *
 * @since 2.0.0
 */
class ScannerModuleDescriptorParser implements ModuleDescriptorParser {

    private static final Pattern delimiter = Pattern.compile("\\s+|(?<=[{};])|(?=[{};])");

    @Override
    public ModuleDescriptor parse(InputStream input) {
        ModuleDescriptor.Builder builder = null;
        try (Scanner scanner = new Scanner(input)) {
            scanner.useDelimiter(delimiter);

            Set<ModuleDescriptor.Modifier> modifiers = new HashSet<>();
            while (scanner.hasNext()) {
                String token = scanner.next();
                if ("module".equals(token)) {
                    break;
                } else if ("open".equals(token)) {
                    modifiers.add(ModuleDescriptor.Modifier.OPEN);
                } else {
                    // ignore
                }
            }

            if (scanner.hasNext()) {
                String moduleName = scanner.next();
                builder = ModuleDescriptor.newModule(moduleName, modifiers);
            }

            if (scanner.hasNext() && "{".equals(scanner.next())) {
                // enter body
            }

            while (scanner.hasNext()) {
                String token = scanner.next();
                if ("}".equals(token)) {
                    break;
                } else if ("requires".equals(token)) {
                    Set<Requires.Modifier> requiresModifiers = new HashSet<>();
                    while (scanner.hasNext()) {
                        String reqToken = scanner.next();
                        if (";".equals(reqToken)) {
                            break;
                        }
                        if ("transitive".equals(reqToken)) {
                            requiresModifiers.add(Requires.Modifier.TRANSITIVE);
                        } else if ("static".equals(reqToken)) {
                            requiresModifiers.add(Requires.Modifier.STATIC);
                        } else {
                            builder.requires(requiresModifiers, reqToken);
                        }
                    }
                    //
                } else {
                    // ignore
                }
            }
        }
        return builder.build();
    }
}
