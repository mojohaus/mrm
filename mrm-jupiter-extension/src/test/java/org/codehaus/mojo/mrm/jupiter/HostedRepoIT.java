package org.codehaus.mojo.mrm.jupiter;

import java.nio.file.Files;
import java.nio.file.Path;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@MockRepositoryManager(hostedRepos = @HostedRepo(target = @Directory("target/hosted-repo")))
class HostedRepoIT {

    @Test
    void mainArtifact(MockRepositoryManagerServer server) throws Exception {
        final String mainArtifactUrl = server.getArtifactUrl("dev.groupid", "artifactid", "4.2", "txt");

        RestAssured.given()
                .body("Downloaded artifactid-4.2.txt successfully")
                .contentType(ContentType.TEXT)
                .put(mainArtifactUrl)
                .then()
                .statusCode(200);

        String mainContent =
                Files.readString(Path.of("target/hosted-repo/dev/groupid/artifactid/4.2/artifactid-4.2.txt"));

        assertThat(mainContent).isEqualTo("Downloaded artifactid-4.2.txt successfully");
    }

    @Test
    void classifiedArtifact(MockRepositoryManagerServer server) throws Exception {
        final String classifierArtifactUrl =
                server.getArtifactUrl("dev.groupid", "artifactid", "4.2", "properties", "meta");

        RestAssured.given()
                .body("download.artifactid-4.2-meta.properties=success")
                .contentType(ContentType.TEXT)
                .put(classifierArtifactUrl)
                .then()
                .statusCode(200);

        String classifiedContent = Files.readString(
                Path.of("target/hosted-repo/dev/groupid/artifactid/4.2/artifactid-4.2-meta.properties"));

        assertThat(classifiedContent).isEqualTo("download.artifactid-4.2-meta.properties=success");
    }
}
