package org.codehaus.mojo.mrm.jupiter;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MockRepositoryManager(localRepos = @LocalRepo(source = @Directory("src/it/resources/local-repo/repositories")))
public class LocalRepoIT {

    @Test
    void mainArtifact(MockRepositoryManagerServer server) {
        final String mainArtifactUrl = server.getArtifactUrl("dev.groupid", "artifactid", "4.2", "txt");

        String mainContent = RestAssured.get(mainArtifactUrl)
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertEquals("Downloaded artifactid-4.2.txt successfully", mainContent);
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

        assertEquals("downdload.artifactid-4.2-meta.properties=success", classifiedContent);
    }
}
