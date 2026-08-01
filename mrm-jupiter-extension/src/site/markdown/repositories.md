title: Repository Types

<!---
Copyright MojoHaus and contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

Repository Types
================

The `MockRepositoryManager` annotation allows you to configure one or more repository types that the Mock Repository Manager will serve. When multiple repositories are specified, they are merged into a single view.

Make sure at least one repository is configured.

## Available Repository Types

### @MockRepo

A mock Maven repository that serves content from a local directory structure with specific file patterns.

**Fields:**

* `source` (required) - The directory containing the mock repository content
* `cloneTo` (optional) - Clone the source to a specific directory (useful for directory-based archives)
* `cloneClean` (optional) - Ensure the cloneTo folder is cleaned before every run (default: false)
* `lazyArchiver` (optional) - Set to `false` to archive directories at startup, or `true` to archive when used (default: false)
* `transformDirectiveSource` (optional) - Set the name of the mechanism to transform files in case of a directory based archive. Possible values: `metadata` (default: null)

**Example:**

```java
@MockRepositoryManager(
    mockRepos = @MockRepo(
        source = @Directory("src/it/mrm/repository"),
        cloneTo = @Directory("target/mock-repo"),
        cloneClean = true,
        lazyArchiver = false,
        transformDirectiveSource = MetadataTransformDirectiveFactory.class)
    )
)
```

**Supported File Patterns:**

The mockRepo recognizes the following file patterns in the source directory:

* `**/*.pom` - Maven POM files
* `**/*-{classifier}.{type}` - Artifacts with classifiers (e.g., `mojo-parent-10-site.xml`)
* `**/*.{archiver extension}` - Directory archives using the GAV from the corresponding POM
* `**/*-{classifier}.{archiver extension}` - Directory archives with classifiers
* `archetype-catalog.xml` - Archetype catalog

Supported archiver extensions include: `jar`, `zip`, `tar.gz`, `tgz`, `tar.xz`, and other formats supported by [Plexus Archiver](https://codehaus-plexus.github.io/plexus-archiver/).

**Supported TransformDirectiveSource:**

* `MetadataTransformDirectiveFactory.class` - Put a file called `.mrm-transform.properties` in the root of an archive directory (e.g. `artifactId-1.0.0.jar/.mrm-transform.properties`). This file supports the following entries
  * [input].targetName = [output]
  * [input].contentTransformers = javaToClass

  The `javaToClass` currently supports transforming `module-info.java`, only including `[open]* module MODULENAME` and `requires [static|transitive]* MODULENAME`.

### @LocalRepo

A locally stored Maven repository that serves content from a directory on the filesystem.

**Fields:**

* `source` (required) - The directory containing the local repository (standard Maven repository layout)

**Example:**

```java
@MockRepositoryManager(
    localRepos = @LocalRepo(
        source = @Directory("src/it/resources/local-repo/repositories")
    )
)
```

**Use Cases:**

* Using an existing local repository as a test repository
* Combining with other repository types to provide additional artifacts

### @HostedRepo

A repository used for distribution management that accepts uploaded artifacts (writable repository).

**Parameters:**

* `target` (required) - The directory where uploaded files will be stored

**Example:**

```java
@MockRepositoryManager(
		hostedRepos = @HostedRepo(
				target = @Directory("target/hosted-repo")
		)
)

```

**Use Cases:**

* Testing artifact deployment
* Testing distribution management configuration
* Verifying that artifacts are correctly uploaded

**Note:** This repository type is writable, unlike the other repository types which are read-only.

### @RemoteArtifactSystem

A remoteArtifactSystem serves content from remote repositories.

**Fields:**

* `cacheDirectory` (required) - The directory where uploaded files will be stored

* `repositories` (required) - The remote repositories to use when trying to locate an artifact

**Example:**

```java
@MockRepositoryManager(
        remoteArtifactSystem =
                @RemoteArtifactSystem(
                        cacheDirectory = @Directory("target/remote-repositories"),
                        repositories =
                                @RemoteRepo(
                                        id = "central",
                                        type = "default",
                                        url = "https://repo.maven.apache.org/maven2")))
```

**Use Cases:**

This is useful for providing access to Maven Central and other remote repositories during integration tests.

## Combining Multiple Repositories

You can combine multiple repository types to create a composite view:

```java
@MockRepositoryManager(
		mockRepos = @MockRepo(
				source = @Directory("src/it/mrm/repository")), 
		localRepos = @LocalRepo(
				source = @Directory("src/it/resources/local-repo/repositories")), 
		hostedRepos = @HostedRepo(
				target = @Directory("target/hosted-repo")), 
		remoteArtifactSystem = @RemoteArtifactSystem(
				cacheDirectory = @Directory("target/remote-repositories"), 
				repositories = @RemoteRepo(
						id = "central", 
						type = "default", 
						url = "https://repo.maven.apache.org/maven2")))
```

When multiple repositories are configured, the Mock Repository Manager searches them in the following order:

1. mockRepos

2. localRepos

3. hostedRepos

4. remoteArtifactSystem

