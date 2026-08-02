package org.codehaus.mojo.mrm.jupiter;

import java.nio.file.Files;
import java.nio.file.Path;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@MockRepositoryManager(
        remoteArtifactSystem =
                @RemoteArtifactSystem(
                        cacheDirectory = @Directory("target/remote-repositories"),
                        repositories =
                                @RemoteRepo(
                                        id = "central",
                                        type = "default",
                                        url = "https://repo.maven.apache.org/maven2")))
class RemoteArtifactSystemIT {

    @Test
    void mainArtifact(MockRepositoryManagerServer server) throws Exception {
        Path artifactPath = Path.of("target/remote-repositories", "org/codehaus/mojo/mrm/1.7.1/mrm-1.7.1.pom");

        if (Files.exists(artifactPath)) {
            Files.delete(artifactPath);
        }

        String mainArtifact = server.getArtifactUrl("org.codehaus.mojo", "mrm", "1.7.1", "pom");
        RestAssured.get(mainArtifact).then().statusCode(200);

        assertThat(artifactPath).exists();
    }
}
