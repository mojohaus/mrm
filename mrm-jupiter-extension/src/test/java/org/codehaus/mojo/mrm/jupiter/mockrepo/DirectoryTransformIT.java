/*
 * Copyright 2026 Robert Scholte
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
package org.codehaus.mojo.mrm.jupiter.mockrepo;

import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import io.restassured.RestAssured;
import org.codehaus.mojo.mrm.impl.transform.metadata.MetadataTransformDirectiveFactory;
import org.codehaus.mojo.mrm.jupiter.Directory;
import org.codehaus.mojo.mrm.jupiter.MockRepo;
import org.codehaus.mojo.mrm.jupiter.MockRepositoryManager;
import org.codehaus.mojo.mrm.jupiter.MockRepositoryManagerServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MockRepositoryManager(
        mockRepos =
                @MockRepo(
                        source = @Directory("src/it/resources/mock-repo/directory-transform"),
                        cloneTo = @Directory("target/it/mock-repo/directory-transform"),
                        transformDirectiveSource = MetadataTransformDirectiveFactory.class))
public class DirectoryTransformIT {

    @Test
    void moduleDescriptor(MockRepositoryManagerServer server) throws Exception {
        final String artifactPomUrl = server.getUrl("localhost", "directory-transform", "1.0", "pom");

        RestAssured.get(artifactPomUrl).then().statusCode(200);

        final String artifactJarUrl = server.getUrl("localhost", "directory-transform", "1.0", "jar");

        InputStream jarInputStream = RestAssured.get(artifactJarUrl)
                .then()
                .statusCode(200)
                .extract()
                .response()
                .asInputStream();

        try (JarInputStream inputStream = new JarInputStream(jarInputStream)) {

            boolean hasClass = false;
            boolean hasJava = false;

            JarEntry entry;
            while ((entry = inputStream.getNextJarEntry()) != null) {
                String name = entry.getName();

                if (name.equals("module-info.class")) {
                    hasClass = true;
                } else if (name.equals("module-info.java")) {
                    hasJava = true;
                }
            }

            assertFalse(hasJava, "module-info.java not expected to be in the jar file");
            assertTrue(hasClass, "module-info.class expected to be in the jar file");
        }
    }
}
