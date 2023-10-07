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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Objects;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.maven.ProxyArtifactStore;

/**
 * A proxy repository serving the content from the current Maven session.
 *
 * @since 1.0
 */
@Named("proxyRepo")
@Singleton
public class ProxyRepo implements ArtifactStoreFactory {

    private FactoryHelper factoryHelper;

    /**
     * Constructor used by Plexus configuration injector.
     * Note: since it does not provide the {@link FactoryHelper} instance, the instance needs
     * to be provided using {@link #setFactoryHelper(FactoryHelper)}
     */
    public ProxyRepo() {}

    /**
     * Injects the singleton instance
     * @param factoryHelper injected {@link FactoryHelper} instance
     */
    @Inject
    public ProxyRepo(FactoryHelper factoryHelper) {
        this.factoryHelper = factoryHelper;
    }

    @Override
    public ArtifactStore newInstance(MavenSession session, Log log) {
        return new ProxyArtifactStore(
                Objects.requireNonNull(factoryHelper, "FactoryHelper has not been set"), session, log);
    }

    @Override
    public void setFactoryHelper(FactoryHelper factoryHelper) {
        this.factoryHelper = factoryHelper;
    }

    @Override
    public String toString() {
        return "Proxy (source: this Maven session)";
    }
}
