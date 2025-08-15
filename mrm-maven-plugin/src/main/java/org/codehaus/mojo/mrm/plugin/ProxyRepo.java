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

import java.util.Objects;

import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.maven.ProxyArtifactStore;

/**
 * A proxy repository serving the content from the current Maven session.
 *
 * @since 1.0
 */
public class ProxyRepo implements ArtifactStoreFactory {

    @Override
    public ArtifactStore newInstance(FactoryHelper factoryHelper) {
        return new ProxyArtifactStore(Objects.requireNonNull(factoryHelper, "FactoryHelper has not been set"));
    }

    @Override
    public String toString() {
        return "Proxy (source: this Maven session)";
    }
}
