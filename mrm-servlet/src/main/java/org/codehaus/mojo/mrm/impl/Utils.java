/*
 * Copyright 2009-2011 Stephen Connolly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.mojo.mrm.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Utility class.
 *
 * @since 1.0
 */
public final class Utils {
    /**
     * Do not instantiate.
     *
     * @since 1.0
     */
    private Utils() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Creates an empty jar file.
     *
     * @return the empty jar file as a byte array.
     * @throws IOException if things go wrong.
     * @since 1.0
     */
    public static byte[] newEmptyJarContent() throws IOException {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Archiver-Version", "1.0");
        manifest.getMainAttributes().putValue("Created-By", "Mock Repository Maven Plugin");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new JarOutputStream(bos, manifest).close();
        return bos.toByteArray();
    }

    /**
     * Creates an empty maven plugin jar file.
     *
     * @param groupId    the group id of the plugin.
     * @param artifactId the artifact id of the plugin.
     * @param version    the version of the plugin.
     * @return the empty jar file as a byte array.
     * @throws IOException if things go wrong.
     * @since 1.0
     */
    public static byte[] newEmptyMavenPluginJarContent(String groupId, String artifactId, String version)
            throws IOException {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Archiver-Version", "1.0");
        manifest.getMainAttributes().putValue("Created-By", "Mock Repository Maven Plugin");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(bos, manifest)) {
            JarEntry entry = new JarEntry("META-INF/maven/plugin.xml");
            jos.putNextEntry(entry);
            jos.write(("<plugin><groupId>" + groupId + "</groupId><artifactId>" + artifactId + "</artifactId><version>"
                            + version
                            + "</version></plugin>")
                    .getBytes());
            jos.closeEntry();
        }
        return bos.toByteArray();
    }
}
