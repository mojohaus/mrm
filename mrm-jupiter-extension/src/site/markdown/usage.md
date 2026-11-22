title: Usage
author: Robert Scholte
date: 2026-07-27

<!---
Copyright 2026 Robert Scholte

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

Usage
=====

The jupiter extension offers annotations for hosting a mock repository manager as part of a Jupiter Test Lifecycle.

```xml
<project>
  ...
  <dependencies>
    ...
      <dependency>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>mrm-jupiter-extension</artifactId>
        <version><!-- latest release --></version>
        <scope>test</scope>
      </dependency>
    ...
  </dependencies>
  ...
</project>
```

Basic Usage
-----------

Write your unit test as usual and annotate the class with @MockRepositoryManager.
Tests that depend on the mock repository manager should add the MockRepositoryManagerServer parameter to their method.

To make use of of it, start writing a unit test as follows:

```java
@MockRepositoryManager
class ExampleTest {

  @Test
  void example(MockRepositoryManagerServer server) {
  
  }
}
```

Make sure to add at least 1 repository, see [Repository Types](./repositories.html) where every different type is explained.
Without one, the extension fails with `At least 1 repository required`.

The server listens on a random free port. To fix the port or serve the repository below a path, set the `port` and
`basePath` attributes of the annotation, for example `@MockRepositoryManager(port = 8080, basePath = "/repo")`.

The `MockRepositoryManagerServer` parameter gives access to the running server:

* `getUrl()` returns the base URL of the server.
* `getArtifactUrl(groupId, artifactId, version, extension)` and the variant with a classifier return the URL of an
  artifact. `getMetadataUrl(...)` does the same for `maven-metadata.xml` and other metadata files.
* `getSettingsFile()` returns the path of a generated temporary Maven `settings.xml` that mirrors every repository to
  the server. Pass it to a forked Maven build with `-s`.

Paths
-----

Every `@Directory` is relative to the working directory of the test (`user.dir`). To start from another base, set
`baseBathResolver` to `UserHomePathResolver.class`, or to your own `PathResolver` with a public no-argument
constructor. A path must be relative and must not leave its base directory.

Security
--------

By default the Mock Repository Manager is not secured, meaning that every for every http call you don't need to be authorized or even authenticated.
Every existing resource will be served.

By adding users to the MockRepositoryManager annotation, every http call must contain a basic authorization header to access the resources.

```java
@MockRepositoryManager(
        users = @User(username = "john.doe", password = "s3cr3t"))
class ExampleTest {

  @Test
  void example(MockRepositoryManagerServer server) {
  
  }
}
```

