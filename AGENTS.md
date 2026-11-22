# Agent guide for mrm, 1.x line

This file is for automated agents and human contributors working on the `mrm-1.x` branch. It
records what the code and the POMs do not make obvious. `master` is the 2.x line; this branch
keeps Java 8 compatibility for projects that cannot move to Java 17.

## Modules

|       Module       |                                                           Contents                                                           |
|--------------------|------------------------------------------------------------------------------------------------------------------------------|
| `mrm-api`          | The file system and artifact store interfaces, and the factory SPI in `plugin` (`FileSystemFactory`, `ArtifactStoreFactory`) |
| `mrm-servlet`      | The servlet that serves a mock repository, and the artifact store implementations behind it                                  |
| `mrm-maven-plugin` | The `start`, `stop` and `run` goals, the embedded Jetty server (`FileSystemServer`) and the integration tests                |

There is no `mrm-servlet-jetty` module and no JUnit Jupiter extension on this line; both exist
on `master` only.

## Build gates

- `spotless-maven-plugin` from mojo-parent runs during `process-sources`. Run `mvn spotless:apply`
  before committing. It formats Java with palantir-java-format, sorts the POM, and also reformats
  Markdown, including this file and the README.
- The build compiles with `mojo.java.target` 8, so the code and the tests must stay Java 8
  compatible. The CI matrix also builds on JDK 17, 21 and 25.
- The modules compile against `mavenVersion` from the root POM, which tracks the Maven 3.9 line.
  The root POM sets `minimalMavenBuildVersion` for building the project, and `mrm-maven-plugin`
  declares a literal Maven 3.9.0 as its `<prerequisites>`, the version users need. Raising
  `mavenVersion` must not raise the prerequisite.
- Fields of the mojo classes are plugin parameters; renaming one changes the plugin's
  configuration API.

## Unit tests

- JUnit 5, under each module's `src/test/java`, with Mockito; the Mockito version is pinned to
  the last one that runs on Java 8.

## Integration tests

- The plugin's tests are in `mrm-maven-plugin/src/it` and run with the `run-its` profile, which is
  declared in that module. Each directory there is a project that starts a mock repository with
  the plugin, and runs a second invoker build, from its own `src/it`, against the repository
  URL it passes in as `mrm.repository.url`. Run one with:

  ```shell
  mvn -B -P run-its verify -pl mrm-maven-plugin -am -DskipTests -Dinvoker.test=custom-base-path
  ```

  `-am` builds the other modules first, because the plugin depends on them. The projects are
  `custom-base-path`, `hostedrepo` and `security`.

- Write `verify.groovy` as `assert` statements. Invoker fails a script whose last expression is
  anything other than `true` or `null`, so a trailing `list.each { }` fails the test; use a `for`
  loop.

- The settings files under `src/it` take the repository URL from the `@mrm.repository.url@`
  filter, which the running mock repository fills in.

## Continuous integration

- `.github/workflows/maven.yml` calls the shared MojoHaus verify workflow in `mojohaus/.github`
  with JDK 8, 17, 21 and 25 (the workflow is shared with `master`, which builds without JDK 8);
  macOS and Windows run on fewer JDKs.
- Branches pushed to this repository build under the `push` event.

## Dependencies

- Dependabot updates are daily. Jetty stays below 10 in `.github/dependabot.yml`, because Jetty
  10 and later need Java 11.

## Documentation updates

A change that users or contributors can notice ships with the documentation it affects, in the
same pull request. A refactor with no visible effect needs none.

- A new goal, parameter or default of `mrm-maven-plugin`: the mojo Javadoc, which becomes the goal
  page, and the goal list in `mrm-maven-plugin/src/site/markdown/index.md`.
- A new repository type or option: `repositories.md` in `mrm-maven-plugin/src/site/markdown`, and
  the Javadoc of the repository class in `mrm-servlet`. Give `@since` the 1.x release that first
  ships it.
- New or changed usage: `usage.md` of `mrm-maven-plugin`. Only the first plugin snippet carries a
  version, written as `<version><!-- latest release --></version>`; the others leave it out, so no
  example goes stale.
- The invoker examples `examples/invoker-tests.md.vm` and `invoker-tests-dist.md.vm` stay Velocity
  pages, because they read `maven-invoker-plugin.version` from the POM with `$context.get`. Escape a
  literal `${...}` in them as `\${...}`.
- A page added, removed or renamed: the menu in that module's `src/site/site.xml`. Never rename a
  published page; its URL is linked from elsewhere.
- The Maven or Java requirement, or how to build and release: `README.md`.
- A new convention, build gate or test fixture: this file.
- A fix or feature that also applies to the other line: port it and its documentation, so that the
  docs of `master` (2.x) and this branch (1.x) stay consistent. The two lines differ in the Java
  floor, the module layout and the 2.x-only features (Jupiter extension, content
  transformations); do not document those here.

The site is published from `master` only. Check the result of a change here with `mvn site` and
open `target/site/index.html` of the module you changed.

## Pull requests

- Open pull requests against `mrm-1.x`. Squash and rebase merges are enabled; merge commits are
  not. Release notes for this line are written by hand when the release is published, so give
  the pull request a title that describes the change for a user.

