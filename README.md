# Mock Repository Manager: Project

Mock Repository Manager for Maven. The Mock Repository Manager provides a mock Maven
repository manager.
 
[![Build Status](https://travis-ci.org/mojohaus/mrm.svg?branch=master)](https://travis-ci.org/mojohaus/mrm)

## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site site:stage scm-publish:publish-scm
```
