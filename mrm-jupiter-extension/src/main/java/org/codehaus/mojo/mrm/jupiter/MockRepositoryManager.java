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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotation to make use the mock repository manager.
 * Its options should be similar to the mrm-maven-plugin configuration
 *
 * @since 2.0.0
 */
@Retention(RUNTIME)
@Target(TYPE)
@ExtendWith(MockRepositoryManagerExtension.class)
public @interface MockRepositoryManager {

    /**
     * The port to start the server on. Defaults to {@code 0} which means a random available port will be chosen.
     *
     * @return the port number
     */
    int port() default 0;

    /**
     * The base context path for the server. Defaults to {@code "/"}.
     *
     * @return the context path
     */
    String basePath() default "/";

    /**
     * Mock repositories whose artifact content is derived from POM files in the source directory.
     *
     * @return the mock repository configurations
     * @see MockRepo
     */
    MockRepo[] mockRepos() default {};

    /**
     * Locally stored Maven repositories served read-only from a directory on disk.
     *
     * @return the local repository configurations
     * @see LocalRepo
     */
    LocalRepo[] localRepos() default {};

    /**
     * Hosted repositories that accept artifact uploads (distribution management).
     *
     * @return the hosted repository configurations
     * @see HostedRepo
     */
    HostedRepo[] hostedRepos() default {};

    /**
     * The remote repositories for downloading artifacts. Within the Maven context these are
     * the repositories defined in the pom.xml, having central defined as the default.
     *
     * @return the local repository configurations
     * @see LocalRepo
     */
    RemoteRepositories remoteRepositories() default
            @RemoteRepositories(
                    cacheDirectory = @Directory(value = ""),
                    repositories = {});
}
