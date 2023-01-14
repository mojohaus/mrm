title: Introduction
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

# Mock Repository Manager Maven Plugin

The Mock Repository Manager Plugin is used when you want to test Maven plugins against specific sets of dependencies
in a repository.

## Goals Overview

The Mock Repository Manager Plugin has the following goals.

* [mrm:run](./run-mojo.html) Runs a Mock Repository for developer manual testing.
* [mrm:start](./start-mojo.html) Starts a Mock Repository as a daemon process for the lifetime of the build (or until stopped)
* [mrm:stop](./stop-mojo.html) Stops a previously started Mock Repository daemon process.

## Usage

General instructions on how to use the Mock Repository Manager Plugin can be found on the [usage page](./usage.html).
Some more specific use cases are described in the examples given below.

In case you still have questions regarding the plugin's usage, please have feel free to contact the [user mailing list](./mailing-lists.html). The posts to the mailing list are archived and could
already contain the answer to your question as part of an older thread. Hence, it is also worth browsing/searching the [mail archive](./mailing-lists.html).

If you feel like the plugin is missing a feature or has a defect, you can fill a feature request or bug report in our [issue tracker](./issue-management.html). When creating a new issue, please provide a comprehensive description of your concern. Especially for fixing bugs it is crucial that the developers can reproduce your problem. For this reason, entire debug logs, POMs or most preferably little demo projects attached to the issue are very much appreciated.

Of course, patches are welcome, too. Contributors can check out the project from our [guide to helping with Maven source repository](./scm.html) and will find supplementary information in the https://maven.apache.org/guides/development/guide-helping.html).

## Examples

To provide you with better understanding of some usages of the Plugin Name,
you can take a look into the following examples:

* [Using with Maven Invoker with repositories](./examples/invoker-tests.html)
* [Using with Maven Invoker with distributionManagement](./examples/invoker-tests-dist.html)
