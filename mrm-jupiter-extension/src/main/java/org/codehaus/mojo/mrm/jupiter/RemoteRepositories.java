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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @since 2.0.0
 */
@Retention(RUNTIME)
@Target({})
public @interface RemoteRepositories {

    /**
     * The cache directory for all remote repositories
     *
     * For using Mavens default localRepository, set it to {@code @Directory( value = ".m2/repository", baseBathResolver = UserHomePathResolver.class ) }
     *
     * @return the shared
     *
     * @see UserHomePathResolver
     */
    Directory cacheDirectory();

    /**
     * The remote repositories, which is empty by default, not even the link to Maven Central
     *
     * For Maven projects, the default remote repository (Maven Central) is configured in the super-pom as
     * provided by the Maven buildtool itself.
     *
     * Since this extension is not aware of any buildtool, you need it to provide it yourself, e.g. {@code @RemoteRepo( id = "central", "type" = "default", url = "https://repo.maven.apache.org/m2" )}
     *
     * @return the remote repositories
     */
    RemoteRepo[] repositories();
}
