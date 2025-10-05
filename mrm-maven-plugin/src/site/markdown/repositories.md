title: Repository Types
author: Stephen Connolly
date: 2025-01-01

<!---
Copyright 2011 Stephen Connolly

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

The `repositories` parameter allows you to configure one or more repository types that the Mock Repository Manager will serve. When multiple repositories are specified, they are merged into a single view.

If no repositories are specified, a `proxyRepo` is used by default to proxy through the current Maven session.

## Available Repository Types

### mockRepo

A mock Maven repository that serves content from a local directory structure with specific file patterns.

**Parameters:**

* `source` (required) - The directory containing the mock repository content
* `cloneTo` (optional) - Clone the source to a specific directory (useful for directory-based archives)
* `cloneClean` (optional) - Ensure the cloneTo folder is cleaned before every run (default: false)
* `lazyArchiver` (optional) - Set to `false` to archive directories at startup, or `true` to archive when used (default: false)

**Example:**

```xml
<repositories>
  <mockRepo>
    <source>src/it/mrm/repository</source>
    <cloneTo>target/mock-repo</cloneTo>
    <cloneClean>true</cloneClean>
    <lazyArchiver>false</lazyArchiver>
  </mockRepo>
</repositories>
```

**Supported File Patterns:**

The mockRepo recognizes the following file patterns in the source directory:

* `**/*.pom` - Maven POM files
* `**/*-{classifier}.{type}` - Artifacts with classifiers (e.g., `mojo-parent-10-site.xml`)
* `**/*.{archiver extension}` - Directory archives using the GAV from the corresponding POM
* `**/*-{classifier}.{archiver extension}` - Directory archives with classifiers
* `archetype-catalog.xml` - Archetype catalog

Supported archiver extensions include: `jar`, `zip`, `tar.gz`, `tgz`, `tar.xz`, and other formats supported by [Plexus Archiver](https://codehaus-plexus.github.io/plexus-archiver/).

### localRepo

A locally stored Maven repository that serves content from a directory on the filesystem.

**Parameters:**

* `source` (required) - The directory containing the local repository (standard Maven repository layout)

**Example:**

```xml
<repositories>
  <localRepo>
    <source>${project.build.directory}/local-repo</source>
  </localRepo>
</repositories>
```

**Use Cases:**

* Serving artifacts installed by `maven-invoker-plugin:install`
* Using an existing local repository as a test repository
* Combining with other repository types to provide additional artifacts

### proxyRepo

A proxy repository that serves content from the current Maven session's configured repositories.

**Parameters:**

None - this repository type has no configuration parameters.

**Example:**

```xml
<repositories>
  <proxyRepo/>
</repositories>
```

**Behavior:**

When used, the proxyRepo forwards requests to all repositories configured in the current Maven build, including:
* Repositories from the POM
* Repositories from active profiles
* Repositories from settings.xml

This is useful for providing access to Maven Central and other remote repositories during integration tests.

### hostedRepo

A repository used for distribution management that accepts uploaded artifacts (writable repository).

**Parameters:**

* `target` (required) - The directory where uploaded files will be stored

**Example:**

```xml
<repositories>
  <hostedRepo>
    <target>target/hosted-repo</target>
  </hostedRepo>
</repositories>
```

**Use Cases:**

* Testing artifact deployment with `mvn deploy`
* Testing distribution management configuration
* Verifying that artifacts are correctly uploaded

**Note:** This repository type is writable, unlike the other repository types which are read-only.

## Combining Multiple Repositories

You can combine multiple repository types to create a composite view:

```xml
<configuration>
  <repositories>
    <!-- Mock repository with test artifacts -->
    <mockRepo>
      <source>src/it/mrm/repository</source>
    </mockRepo>
    
    <!-- Local repository with built artifacts -->
    <localRepo>
      <source>${project.build.directory}/local-repo</source>
    </localRepo>
    
    <!-- Proxy to Maven Central and other configured repositories -->
    <proxyRepo/>
  </repositories>
</configuration>
```

When multiple repositories are configured, the Mock Repository Manager searches them in order and returns the first match found.

## Default Configuration

If the `repositories` parameter is not specified or is empty, the plugin defaults to:

```xml
<repositories>
  <proxyRepo/>
</repositories>
```

This provides a pass-through to the current Maven instance's repositories.
