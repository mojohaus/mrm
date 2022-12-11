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

/**
 * Marker interface to indicate that a {@link ArtifactStoreFactory} or {@link FileSystemFactory} should be provided with
 * a {@link FactoryHelper} before use.
 *
 * @since 1.0
 */
public interface FactoryHelperRequired {
    /**
     * Provide the {@link FactoryHelper} instance.
     *
     * @param factoryHelper the {@link FactoryHelper} instance.
     * @since 1.0
     */
    void setFactoryHelper(FactoryHelper factoryHelper);
}
