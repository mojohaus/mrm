title: Introduction
author: Stephen Connolly
date: 2011-11-14

The Mock Repository Manager suite of projects are used to provide mock or lightweight Maven Repository Managers for
use during integration testing of Maven plugins.

This is the 1.x line, which requires Maven 3.9.0 or later and Java 8 or later. The 2.x line needs Java 17 or later.

The suite has these modules:

* [mrm-maven-plugin](./mrm-maven-plugin/index.html) starts and stops a repository from a Maven build, or runs one for manual testing.
* `mrm-api` and `mrm-servlet` hold the repository model and the servlet that serves it.

