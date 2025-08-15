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

import org.codehaus.mojo.mrm.api.maven.ArtifactStore;

/**
 * Something that produces {@link ArtifactStore} instances.
 *
 * @since 1.0
 */
public interface ArtifactStoreFactory {
    /**
     * Creates a new {@link ArtifactStore} instance, note that implementations are free to create a singleton and always
     * return that instance.
     *
     * @param factoryHelper {@link FactoryHelper} instance
     * @return the {@link ArtifactStore} instance.
     * @since 1.0
     */
    default ArtifactStore newInstance(FactoryHelper factoryHelper) {
        return newInstance();
    }

    default ArtifactStore newInstance() {
        throw new UnsupportedOperationException("method newInstance() not implemented");
    }
}
