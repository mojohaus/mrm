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
package org.codehaus.mojo.mrm.jupiter;

import io.restassured.RestAssured;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Based on /mrm-maven-plugin/src/it/custom-base-path/src/it/resolve/src/test/java/org/codehaus/mojo/mrm/plugin/it/resolve/CustomBasePathTest.java
 */
@MockRepositoryManager(
        basePath = "/foo/bar",
        remoteArtifactSystem =
                @RemoteArtifactSystem(
                        cacheDirectory = @Directory(value = ".m2", baseBathResolver = UserHomePathResolver.class),
                        repositories =
                                @RemoteRepo(
                                        id = "central",
                                        type = "default",
                                        url = "https://repo.maven.apache.org/maven2")))
public class CustomBasePathIT {

    @Test
    void customBasePath(MockRepositoryManagerServer server) {
        final String mrmUri = server.getUrl();

        /* Make sure the repo ends with the base path we have set in mrm-maven-plugin/src/it/custom-base-path/pom.xml */
        Assertions.assertThat(mrmUri).endsWith("foo/bar");

        String artifactUrl = server.getArtifactUrl("org.apache.commons", "commons-lang3", "3.12.0", "pom");

        /* Try to download something and make sure the content is as expected */
        String body = RestAssured.get(artifactUrl)
                .then()
                .statusCode(200)
                .extract()
                .response()
                .asString();

        assertTrue(body.contains("<artifactId>commons-lang3</artifactId>"));
        assertTrue(body.contains("<version>3.12.0</version>"));
    }
}
