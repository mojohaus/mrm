# Mock Repository Manager (1.x)

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/mojohaus/mrm.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/mrm-maven-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.codehaus.mojo/mrm-maven-plugin)
[![GitHub CI](https://github.com/mojohaus/mrm/actions/workflows/maven.yml/badge.svg?branch=mrm-1.x)](https://github.com/mojohaus/mrm/actions/workflows/maven.yml?query=branch%3Amrm-1.x)

The Mock Repository Manager (MRM) serves a Maven repository for integration tests of Maven plugins and other tools, built from
directories of test artifacts, an existing local repository or the repositories of the running build. It comes as the
`mrm-maven-plugin`, with the `start`, `stop` and `run` goals.

This is the `mrm-1.x` branch, the maintenance line for Java 8. It requires Maven 3.9.0 or later and Java 8 or later; the latest
release is 1.8.0. Projects on Java 17 or later should use 2.x from the `master` branch, which also has a JUnit Jupiter extension,
content transformations and a restructured module layout. Stay on 1.x if your build runs on a JDK older than 17. Fixes that apply
to both lines are made on `master` first and ported here.

To serve the repositories of the current build for manual testing and stop with ENTER:

```shell
mvn org.codehaus.mojo:mrm-maven-plugin:1.8.0:run -Dmrm.port=8080
```

The [project site](https://www.mojohaus.org/mrm/) is published from `master` and describes 2.x. Build the 1.x site with
`mvn site` on this branch; the usage guides and examples are in `mrm-maven-plugin/src/site/markdown`.

Conventions for contributors and agents are in [AGENTS.md](AGENTS.md).

## Releasing

* Make sure `gpg-agent` is running.
* On this branch, run `mvn -B release:prepare release:perform`.
* The Release Drafter workflow runs for pushes to `master` only, so write the release notes for 1.x by hand, and publish the
  GitHub release without marking it as the latest, so that 2.x stays the latest.
* Do not publish the site from this branch; it is published from `master`.

