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

import javax.inject.Provider;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.impl.digest.AutoDigestFileSystem;
import org.codehaus.mojo.mrm.impl.maven.ArtifactStoreFileSystem;
import org.codehaus.mojo.mrm.impl.maven.CompositeArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.DiskArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.MockArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.ProxyArtifactStore;
import org.codehaus.mojo.mrm.impl.transform.TransformDirectiveSourceFactory;
import org.codehaus.mojo.mrm.jetty.FileSystemServer;
import org.codehaus.mojo.mrm.jetty.FileSystemServerException;
import org.codehaus.mojo.mrm.plugin.FactoryHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ear.EarArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.DefaultArchiverManager;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.StoreScope;
import org.junit.jupiter.api.extension.ExtensionContextException;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * The extension for preparing the mrm-server based on the {@link MockRepositoryManager} annotation and
 * exposing relevant settings via {@link MockRepositoryManagerServer}
 *
 * @since 2.0.0
 */
class MockRepositoryManagerExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(MockRepositoryManagerExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        MockRepositoryManager annotation = context.getRequiredTestClass().getAnnotation(MockRepositoryManager.class);

        ExtensionContext.Store rootStore = context.getStore(StoreScope.EXECUTION_REQUEST, NAMESPACE);

        RepositorySystemHandler repositorySystemSupplier = rootStore.computeIfAbsent(
                RepositorySystemHandler.class, key -> createRepositorySystem(), RepositorySystemHandler.class);

        rootStore.computeIfAbsent(annotation, key -> {
            try {
                return createServerSource(annotation, repositorySystemSupplier);
            } catch (FileSystemServerException e) {
                throw new ExtensionContextException("Failed to create a ServerSource", e);
            }
        });
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(MockRepositoryManagerServer.class);
    }

    @Override
    public @Nullable MockRepositoryManagerServer resolveParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        MockRepositoryManager annotation =
                extensionContext.getRequiredTestClass().getAnnotation(MockRepositoryManager.class);

        ServerResource resource = extensionContext
                .getStore(StoreScope.EXECUTION_REQUEST, NAMESPACE)
                .get(annotation, ServerResource.class);
        if (resource == null) {
            throw new ParameterResolutionException(
                    "MockRepositoryManagerServer is not available. Make sure the test class is annotated with @MockRepositoryManager.");
        }
        return resource.getHandle();
    }

    private RepositorySystemHandler createRepositorySystem() {
        DefaultRepositorySystem repositorySystem = new DefaultRepositorySystem();
        DefaultServiceLocator serviceLocator = MavenRepositorySystemUtils.newServiceLocator();
        serviceLocator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        // Registreer de HTTP transport factory
        serviceLocator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        repositorySystem.initService(serviceLocator);

        return new RepositorySystemHandler(repositorySystem);
    }

    private ServerResource createServerSource(
            MockRepositoryManager mockRepositoryManager, Supplier<RepositorySystem> repoSystemSupplier)
            throws FileSystemServerException {
        FileSystemServer mrm = createFileSystemServer(mockRepositoryManager, repoSystemSupplier);

        mrm.ensureStarted();

        MockRepositoryManagerServer handle = new MockRepositoryManagerServer(mrm.getUrl());

        return new ServerResource(mrm, handle);
    }

    /**
     * Creates a file system server from an artifact store.
     *
     * @param artifactStore the artifact store to serve.
     * @return the file system server.
     */
    private FileSystemServer createFileSystemServer(
            MockRepositoryManager mockRepositoryManager, Supplier<RepositorySystem> repoSystemSupplier) {
        int port = mockRepositoryManager.port();
        String basePath = mockRepositoryManager.basePath();
        ArtifactStore artifactStore = createArtifactStore(mockRepositoryManager, repoSystemSupplier);
        Collection<org.codehaus.mojo.mrm.api.User> users = Arrays.stream(mockRepositoryManager.users())
                .map(MockRepositoryManagerExtension::toUser)
                .toList();

        return new FileSystemServer(
                "mrm-fileserver",
                Math.max(0, Math.min(port, 65535)),
                basePath,
                new AutoDigestFileSystem(new ArtifactStoreFileSystem(artifactStore)),
                users,
                true);
    }

    private ArtifactStore createArtifactStore(
            MockRepositoryManager annotation, Supplier<RepositorySystem> repoSystemSupplier) {
        List<ArtifactStore> stores = new ArrayList<>();

        for (MockRepo mockRepo : annotation.mockRepos()) {
            stores.add(createMockRepoStore(mockRepo));
        }
        for (LocalRepo localRepo : annotation.localRepos()) {
            Path sourceDirectory = DirectoryResolver.resolve(localRepo.source());

            stores.add(new DiskArtifactStore(sourceDirectory.toFile()));
        }
        for (HostedRepo hostedRepo : annotation.hostedRepos()) {
            Path target = DirectoryResolver.resolve(hostedRepo.target());

            Path targetDirectory;
            try {
                targetDirectory = Files.createDirectories(target);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create hosted repository directory: " + target);
            }
            stores.add(new DiskArtifactStore(targetDirectory.toFile()).canWrite(true));
        }

        RemoteArtifactSystem remoteRepositories = annotation.remoteArtifactSystem();

        if (remoteRepositories.repositories().length > 0) {
            List<RemoteRepository> remoteRepos = Arrays.stream(remoteRepositories.repositories())
                    .map(MockRepositoryManagerExtension::toRemoteRepository)
                    .toList();

            DefaultRepositorySystemSession repositorySystemSession = MavenRepositorySystemUtils.newSession();

            LocalRepository cacheDirectory = toLocalRepository(remoteRepositories.cacheDirectory());

            RepositorySystem repositorySystem = repoSystemSupplier.get();
            LocalRepositoryManager localRepositoryManager =
                    repositorySystem.newLocalRepositoryManager(repositorySystemSession, cacheDirectory);

            repositorySystemSession.setLocalRepositoryManager(localRepositoryManager);
            repositorySystemSession.setReadOnly();

            FactoryHelper factoryHelper = new StandaloneFactoryHelper(
                    repositorySystem, repositorySystemSession, remoteRepos, createArchiverManager());

            stores.add(new ProxyArtifactStore(factoryHelper));
        }

        int storeCount = stores.size();
        if (storeCount == 0) {
            throw new IllegalStateException("At least 1 repository required");
        } else if (storeCount == 1) {
            return stores.get(0);
        } else {
            ArtifactStore[] artifactStores = stores.toArray(new ArtifactStore[0]);
            return new CompositeArtifactStore(artifactStores);
        }
    }

    private ArtifactStore createMockRepoStore(MockRepo mockRepo) {
        Path root = DirectoryResolver.resolve(mockRepo.source());

        if (mockRepo.cloneTo().baseBathResolver() != SourcePathResolver.class) {
            Path cloneTarget = DirectoryResolver.resolve(mockRepo.cloneTo());

            if (Files.isDirectory(cloneTarget) && mockRepo.cloneClean()) {
                try {
                    FileUtils.cleanDirectory(cloneTarget);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to clean directory: " + e.getMessage(), e);
                }
            } else if (Files.isRegularFile(cloneTarget)) {
                throw new IllegalStateException("Failed to create clone target directory: " + cloneTarget);
            }

            try {
                Path cloneDirectory = Files.createDirectories(cloneTarget);
                FileUtils.copyDirectory(root, cloneDirectory);
                root = cloneTarget;
            } catch (IOException e) {
                throw new IllegalStateException("Failed to copy directory: " + e.getMessage(), e);
            }
        }

        TransformDirectiveSourceFactory transformDirectiveSourceFactory;
        if (mockRepo.transformDirectiveSource() == NoOpTransformDirectiveSourceFactory.class) {
            transformDirectiveSourceFactory = null;
        } else {
            try {
                transformDirectiveSourceFactory = mockRepo.transformDirectiveSource()
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        mockRepo.transformDirectiveSource() + " is missing default constructor");
            }
        }

        return new MockArtifactStore(
                createArchiverManager(), root.toFile(), mockRepo.lazyArchiver(), transformDirectiveSourceFactory);
    }

    private static ArchiverManager createArchiverManager() {
        Map<String, Provider<Archiver>> archivers = new HashMap<>(4);
        archivers.put("jar", JarArchiver::new);
        archivers.put("zip", ZipArchiver::new);
        archivers.put("war", WarArchiver::new);
        archivers.put("ear", EarArchiver::new);
        return new DefaultArchiverManager(archivers, Map.of(), Map.of());
    }

    private static RemoteRepository toRemoteRepository(RemoteRepo repo) {
        return new RemoteRepository.Builder(repo.id(), repo.type(), repo.url()).build();
    }

    private static LocalRepository toLocalRepository(Directory cacheDirectory) {
        Path targetDirectory = DirectoryResolver.resolve(cacheDirectory);

        return new LocalRepository(targetDirectory.toFile());
    }

    private static org.codehaus.mojo.mrm.api.User toUser(User user) {
        return org.codehaus.mojo.mrm.api.User.of(user.username(), user.password());
    }

    private static final class ServerResource implements AutoCloseable {

        private final FileSystemServer server;
        private final MockRepositoryManagerServer handle;

        ServerResource(FileSystemServer server, MockRepositoryManagerServer handle) {
            this.server = server;
            this.handle = handle;
        }

        MockRepositoryManagerServer getHandle() {
            return handle;
        }

        @Override
        public void close() throws Exception {
            try {
                server.finish();
                server.waitForFinished();
            } finally {
                handle.cleanup();
            }
        }
    }

    private static final class RepositorySystemHandler implements Closeable, Supplier<RepositorySystem> {

        private final DefaultRepositorySystem repositorySystem;

        public RepositorySystemHandler(DefaultRepositorySystem repositorySystem) {
            this.repositorySystem = repositorySystem;
        }

        @Override
        public RepositorySystem get() {
            return repositorySystem;
        }

        @Override
        public void close() throws IOException {
            repositorySystem.shutdown();
        }
    }
}
