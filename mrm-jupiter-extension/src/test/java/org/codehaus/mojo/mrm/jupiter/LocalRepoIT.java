package org.codehaus.mojo.mrm.jupiter;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@MockRepositoryManager(localRepos = @LocalRepo(source = @Directory("src/it/resources/local-repo/repositories")))
class LocalRepoIT {

    @Test
    void mainArtifact(MockRepositoryManagerServer server) {
        final String mainArtifactUrl = server.getArtifactUrl("dev.groupid", "artifactid", "4.2", "txt");

        String mainContent = RestAssured.get(mainArtifactUrl)
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertThat(mainContent).isEqualTo("Downloaded artifactid-4.2.txt successfully");
    }

    @Test
    void classifiedArtifact(MockRepositoryManagerServer server) {
        final String classifierArtifactUrl =
                server.getArtifactUrl("dev.groupid", "artifactid", "4.2", "properties", "meta");

        String classifiedContent = RestAssured.get(classifierArtifactUrl)
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertThat(classifiedContent).isEqualTo("download.artifactid-4.2-meta.properties=success");
    }
}
