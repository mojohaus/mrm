title: Usage
author: Stephen Connolly
date: 2011-11-22

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

Usage
=====

The plugin offers goals for hosting a mock repository manager as part of a Maven project lifecycle.

Basic Usage
-----------

Before you start it is best to add `mrm-maven-plugin` as a plugin in your pom. The plugin requires Maven 3.9.0 or
later and Java 8 or later (1.x line).

```xml
<project>
  ...
  <build>
    ...
    <plugins>
      ...
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>mrm-maven-plugin</artifactId>
        <version><!-- latest release --></version>
      </plugin>
      ...
    </plugins>
    ...
  </build>
  ...
</project>
```

Security
--------

By default the Mock Repository Manager is not secured, meaning that for every http call you don't need to be authorized or even authenticated.
Every existing resource will be served.

By adding users to the configuration of the plugin (available since 1.8.0), every http call must contain a basic authorization header to access the resources.

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>mrm-maven-plugin</artifactId>
  <configuration>
    ...
    <users>
      <user>
        <username>john.doe</username>
        <password>s3cr3t</password>
      </user>
    </users>
  </configuration>
</plugin>
```

Start up a test Mock Repository Manager
---------------------------------------

To start up a test Mock Repository Manager to allow you to develop your tests using your favorite IDE, invoke
the `run` goal. The goal keeps the repository running until you hit ENTER on the console, so it needs an
interactive Maven session and fails in batch mode (`-B`).

```shell
mvn mrm:run
```

You can change the port that the mock repository manager binds to either using the `mrm.port` property from the
command line, e.g.

```shell
mvn mrm:run -Dmrm.port=8080
```

If you do not set a port, a random free one is used. The log shows the URL the repository is served on.

Or in the `configuration` block in the plugin definition (which has the advantage that all invocations will use
the same parameters).

```xml
<project>
  ...
  <build>
    ...
    <plugins>
      ...
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>mrm-maven-plugin</artifactId>
        <configuration>
          <port>8080</port>
        </configuration>
      </plugin>
      ...
    </plugins>
    ...
  </build>
  ...
</project>
```

Serve the repository under a base path
--------------------------------------

By default the repository is served from the root of the server. Set `basePath` (property `mrm.basePath`) to serve it
under another path, for example `/foo/bar`, so that an artifact is available under
`http://localhost:<port>/foo/bar/org/acme/my-artifact/1.2.3/my-artifact-1.2.3.pom`.

Run a repository during the build
---------------------------------

The `start` goal runs in the `pre-integration-test` phase and the `stop` goal in the `post-integration-test` phase.
`start` stores the URL of the repository in the project property named by `propertyName` (property
`mrm.propertyName`, default `mrm.repository.url`), so that a `settings.xml` or a test can use it. `stop` finds the
repository by the id of its execution, so both goals have to run in the same `<execution>` (the `default` execution
when you do not set an id). The [examples](./index.html#Examples) show this together with the Maven Invoker Plugin.

To skip all goals of the plugin, set `mrm.skip` to `true`.
