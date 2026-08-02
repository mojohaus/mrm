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
import org.junit.jupiter.api.Test;

/**
 * This shows how a MRM would be configured with Maven defaults
 */
@MockRepositoryManager(
        localRepos =
                @LocalRepo(
                        source = @Directory(value = ".m2/repositories", baseBathResolver = UserHomePathResolver.class)),
        remoteArtifactSystem =
                @RemoteArtifactSystem(
                        cacheDirectory =
                                @Directory(value = ".m2/repositories", baseBathResolver = UserHomePathResolver.class),
                        repositories =
                                @RemoteRepo(
                                        id = "central",
                                        type = "default",
                                        url = "https://repo.maven.apache.org/maven2")))
class MavenRepositories {

    @Test
    void mainArtifact(MockRepositoryManagerServer server) throws Exception {
        var mainArtifact = server.getArtifactUrl("org.codehaus.mojo", "mrm", "1.7.1", "pom");
        RestAssured.head(mainArtifact).then().statusCode(200);
    }

    @Test
    void classifiedArtifact(MockRepositoryManagerServer server) throws Exception {
        var classifiedArtifact = server.getArtifactUrl("org.codehaus.mojo", "mrm", "1.7.1", "zip", "source-release");
        RestAssured.head(classifiedArtifact).then().statusCode(200);
    }

    @Test
    void groupIdMetadata(MockRepositoryManagerServer server) throws Exception {
        var groupIdMetadata = server.getMetadataUrl("org.codehaus.mojo", "maven-metadata.xml");
        RestAssured.head(groupIdMetadata).then().statusCode(200);
    }

    @Test
    void artifactIdMetadata(MockRepositoryManagerServer server) throws Exception {
        var artifactIdMetadata = server.getMetadataUrl("org.codehaus.mojo", "mrm", "maven-metadata.xml");
        RestAssured.head(artifactIdMetadata).then().statusCode(200);
    }
}
