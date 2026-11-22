title: Introduction
author: Stephen Connolly
date: 2011-11-14

The Mock Repository Manager suite of projects are used to provide mock or lightweight Maven Repository Managers for
use during integration testing of Maven plugins.

The suite has these modules:

* [mrm-maven-plugin](./mrm-maven-plugin/index.html) starts and stops a repository from a Maven build, or runs one for manual testing.
* [mrm-jupiter-extension](./mrm-jupiter-extension/index.html) starts a repository for a JUnit 5 test class.
* `mrm-api`, `mrm-servlet` and `mrm-servlet-jetty` hold the repository model, the servlet that serves it and its embedded Jetty server.

