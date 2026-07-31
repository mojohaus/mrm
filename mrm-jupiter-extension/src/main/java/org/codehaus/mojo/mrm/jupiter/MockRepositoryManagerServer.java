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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This instance will be available as argument for tests for classes marked with {@link MockRepositoryManager}
 *
 * @since 2.0.0
 */
public class MockRepositoryManagerServer {

    private static final String SETTINGS_TEMPLATE_RESOURCE = "/org/codehaus/mojo/mrm/jupiter/settings.xml";

    private static final String SETTINGS_URL_TOKEN = "${mrm.repository.url}";

    private final String url;

    private Path settingsFile;

    MockRepositoryManagerServer(String url) {
        this.url = url;
    }

    /**
     * Returns the base URL of the running Mock Repository Manager server.
     *
     * @return the server URL, e.g. {@code http://localhost:8080}
     */
    public String getUrl() {
        return url;
    }

    /**
     * Calculate the full URL to a specific artifact
     *
     * @param groupId the groupId
     * @param artifactId the artifactId
     * @param version the version
     * @param fileExtension the file extension
     * @return the url to access this artifact
     */
    public String getArtifactUrl(String groupId, String artifactId, String version, String fileExtension) {
        return url
                + '/'
                + groupId.replace('.', '/')
                + '/'
                + artifactId
                + '/'
                + version
                + '/'
                + artifactId
                + '-'
                + version
                + '.'
                + fileExtension;
    }

    /**
     * Calculate the full URL to a specific artifact
     *
     * @param groupId the groupId
     * @param artifactId the artifactId
     * @param version the version
     * @param fileExtension the file extension
     * @param classifier the classifier
     * @return the url to access this artifact
     */
    public String getArtifactUrl(
            String groupId, String artifactId, String version, String fileExtension, String classifier) {
        return url
                + '/'
                + groupId.replace('.', '/')
                + '/'
                + artifactId
                + '/'
                + version
                + '/'
                + artifactId
                + '-'
                + version
                + '-'
                + classifier
                + '.'
                + fileExtension;
    }

    public String getMetadataUrl(String groupId, String file) {
        return url + '/' + groupId.replace('.', '/') + '/' + file;
    }

    public String getMetadataUrl(String groupId, String artifactId, String file) {
        return url + '/' + groupId.replace('.', '/') + '/' + artifactId + '/' + file;
    }

    /**
     * Returns the path to a generated temporary Maven {@code settings.xml} file configured to use this server.
     * The file is created lazily on first call and reused afterwards for this server handle.
     *
     * @return absolute path to the generated Maven settings file
     */
    public synchronized Path getSettingsFile() {
        if (settingsFile == null) {
            try {
                settingsFile = createSettingsFile();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return settingsFile;
    }

    synchronized void cleanup() {
        if (settingsFile != null) {
            try {
                Files.deleteIfExists(settingsFile);
            } catch (IOException cleanupError) { // best-effort cleanup; deleteOnExit fallback avoids leaking temp files
                settingsFile.toFile().deleteOnExit();
            }
            settingsFile = null;
        }
    }

    private Path createSettingsFile() throws IOException {
        Path tempSettingsFile = Files.createTempFile("mrm-settings-", ".xml");
        tempSettingsFile.toFile().deleteOnExit();

        try (InputStream is = MockRepositoryManagerServer.class.getResourceAsStream(SETTINGS_TEMPLATE_RESOURCE);
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr);
                BufferedWriter writer = Files.newBufferedWriter(tempSettingsFile, StandardCharsets.UTF_8)) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (!isFirstLine) {
                    writer.newLine();
                }
                isFirstLine = false;

                writer.write(line.replace(SETTINGS_URL_TOKEN, url));
            }
        }

        return tempSettingsFile;
    }
}
