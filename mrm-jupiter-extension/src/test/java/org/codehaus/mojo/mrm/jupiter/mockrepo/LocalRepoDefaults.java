package org.codehaus.mojo.mrm.jupiter.mockrepo;

import io.restassured.RestAssured;
import org.codehaus.mojo.mrm.jupiter.Directory;
import org.codehaus.mojo.mrm.jupiter.LocalRepo;
import org.codehaus.mojo.mrm.jupiter.MockRepositoryManager;
import org.codehaus.mojo.mrm.jupiter.MockRepositoryManagerServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MockRepositoryManager(localRepos = @LocalRepo(source = @Directory("src/it/resources/local-repo/repositories")))
public class LocalRepoDefaults {

    @Test
    void defaults(MockRepositoryManagerServer server) {
        final String mainArtifactUrl = server.getUrl("dev.groupid", "artifactid", "4.2", "txt");

        String mainContent = RestAssured.get(mainArtifactUrl)
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertEquals("Downloaded artifactid-4.2.txt successfully", mainContent);

        final String classifierArtifactUrl = server.getUrl("dev.groupid", "artifactid", "4.2", "properties", "meta");

        String classifiedContent = RestAssured.get(classifierArtifactUrl)
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertEquals("downdload.artifactid-4.2-meta.properties=success", classifiedContent);
    }
}
