# Mock Repository Manager: Project

Mock Repository Manager for Maven. The Mock Repository Manager provides a mock Maven
repository manager.
 
[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/mojohaus/mrm.svg?label=License)](http://www.apache.org/licenses/)
[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/mrm.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.codehaus.mojo/mrm)
[![GitHub CI](https://github.com/mojohaus/mrm/actions/workflows/maven.yml/badge.svg)](https://github.com/mojohaus/mrm/actions/workflows/maven.yml)

## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn site
mvn scm-publish:publish-scm
```
