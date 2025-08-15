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

package org.codehaus.mojo.mrm.plugin;

import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;

/**
 * Helper interface that exposes the Maven components that may be required.
 *
 * @since 1.0
 */
public interface FactoryHelper {
    /**
     * @return returns the {@link RepositorySystem} instance
     */
    RepositorySystem getRepositorySystem();

    /**
     * @return returns the {@link ArchetypeManager} instance
     * @since 1.0
     */
    ArchetypeManager getArchetypeManager();

    /**
     * @return returns the current {@link MavenSession} instance
     */
    MavenSession getMavenSession();
}
