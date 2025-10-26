package org.codehaus.mojo.mrm.plugin;

/*
 * Copyright 2011 Stephen Connolly
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

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.ArtifactStoreFileSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for FileSystemServer authentication functionality.
 */
@ExtendWith(MockitoExtension.class)
class FileSystemServerAuthTest {

    @Mock
    private ArtifactStore artifactStore;

    private FileSystemServer server;

    @AfterEach
    void tearDown() {
        if (server != null && !server.isFinished()) {
            server.finish();
        }
    }

    @Test
    void testBasicAuthentication() throws Exception {
        // Create server with authentication
        server = new FileSystemServer(
                "test-auth", 0, "/", new ArtifactStoreFileSystem(artifactStore), false, "testuser", "testpass");
        server.ensureStarted();

        // Test without authentication - should get 401
        URL url = new URL(server.getUrl() + "/test.txt");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int responseCode = conn.getResponseCode();
        assertEquals(401, responseCode, "Should receive 401 Unauthorized without credentials");
        conn.disconnect();

        // Test with wrong credentials - should get 401
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        String wrongAuth = "testuser:wrongpass";
        String encodedWrongAuth = Base64.getEncoder().encodeToString(wrongAuth.getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", "Basic " + encodedWrongAuth);
        responseCode = conn.getResponseCode();
        assertEquals(401, responseCode, "Should receive 401 Unauthorized with wrong credentials");
        conn.disconnect();

        // Test with correct credentials - should not be 401
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        String auth = "testuser:testpass";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        responseCode = conn.getResponseCode();
        // We expect 404 since the artifact doesn't exist, but importantly NOT 401
        assertEquals(404, responseCode, "Should receive 404 (not 401) with correct credentials");
        conn.disconnect();
    }

    @Test
    void testNoAuthenticationWhenUsernameNotProvided() throws Exception {
        // Create server without authentication (username is null)
        server = new FileSystemServer(
                "test-no-auth", 0, "/", new ArtifactStoreFileSystem(artifactStore), false, null, null);
        server.ensureStarted();

        // Test without authentication - should not get 401
        URL url = new URL(server.getUrl() + "/test.txt");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int responseCode = conn.getResponseCode();
        // We expect 404 since the artifact doesn't exist, but importantly NOT 401
        assertEquals(404, responseCode, "Should receive 404 (not 401) without authentication when username is not set");
        conn.disconnect();
    }

    @Test
    void testDeprecatedConstructor() throws Exception {
        // Use deprecated constructor (without auth parameters)
        server = new FileSystemServer("test-deprecated", 0, "/", new ArtifactStoreFileSystem(artifactStore), false);
        server.ensureStarted();

        // Should work without authentication
        URL url = new URL(server.getUrl() + "/test.txt");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int responseCode = conn.getResponseCode();
        // We expect 404 since the artifact doesn't exist, but importantly NOT 401
        assertEquals(404, responseCode, "Deprecated constructor should work without authentication");
        conn.disconnect();
    }
}
