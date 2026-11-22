# Mock Repository Manager

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/mojohaus/mrm.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/mrm-maven-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.codehaus.mojo/mrm-maven-plugin)
[![GitHub CI](https://github.com/mojohaus/mrm/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/mojohaus/mrm/actions/workflows/maven.yml?query=branch%3Amaster)

The Mock Repository Manager (MRM) serves a Maven repository for integration tests of Maven plugins and other tools, built from
directories of test artifacts, an existing local repository or the repositories of the running build. It comes as the
`mrm-maven-plugin`, with the `start`, `stop` and `run` goals, and as the `mrm-jupiter-extension` for JUnit 5 tests.

The plugin requires Maven 3.9.0 or later, and all modules require Java 17 or later. To serve the repositories of the
current build for manual testing and stop with ENTER:

```shell
mvn org.codehaus.mojo:mrm-maven-plugin:run -Dmrm.port=8080
```

The [project site](https://www.mojohaus.org/mrm/) has the usage guides and examples for the
[plugin](https://www.mojohaus.org/mrm/mrm-maven-plugin/) and the
[Jupiter extension](https://www.mojohaus.org/mrm/mrm-jupiter-extension/).

Conventions for contributors and agents are in [AGENTS.md](AGENTS.md).

## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn site
mvn scm-publish:publish-scm
```

