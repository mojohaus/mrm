package org.codehaus.mojo.mrm.jupiter;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MockRepositoryManager(
        mockRepos = @MockRepo(source = @Directory("src/it/resources/mock-repo/user")),
        users = @User(username = "john.doe", password = "s3cr3t"))
class UserIT {

    @Test
    void unauthorized(MockRepositoryManagerServer server) throws Exception {
        String artifactUrl = server.getArtifactUrl("localhost", "artifact", "1.0", "pom");

        RestAssured.get(artifactUrl).then().statusCode(401);
    }

    @Test
    void authorized(MockRepositoryManagerServer server) throws Exception {
        String artifactUrl = server.getArtifactUrl("localhost", "artifact", "1.0", "pom");

        RestAssured.given()
                .auth()
                .basic("john.doe", "s3cr3t")
                .get(artifactUrl)
                .then()
                .statusCode(200);
    }
}
