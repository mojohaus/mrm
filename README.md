# Mock Repository Manager: Project

Mock Repository Manager for Maven. The Mock Repository Manager provides a mock Maven
repository manager.
 
[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/mojohaus/mrm.svg?label=License)](http://www.apache.org/licenses/)
[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/mrm.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.codehaus.mojo/mrm)
[![Build Status](https://travis-ci.org/mojohaus/mrm.svg?brnach=master)](https://travis-ci.org/mojohaus/mrm)

## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site site:stage scm-publish:publish-scm
```
