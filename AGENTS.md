# Agent guide for mrm

This file is for automated agents and human contributors working on this repository. It records
what the code and the POMs do not make obvious.

## Modules

|         Module          |                                                           Contents                                                           |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `mrm-api`               | The file system and artifact store interfaces, and the factory SPI in `plugin` (`FileSystemFactory`, `ArtifactStoreFactory`) |
| `mrm-servlet`           | The servlet that serves a mock repository, and the artifact store implementations behind it                                  |
| `mrm-servlet-jetty`     | Embeds `mrm-servlet` in Jetty (`FileSystemServer`)                                                                           |
| `mrm-maven-plugin`      | The `start`, `stop` and `run` goals, and the integration tests that exercise them                                            |
| `mrm-jupiter-extension` | JUnit 5 extension that starts a mock repository for a test                                                                   |

## Build gates

- `spotless-maven-plugin` from mojo-parent runs during `process-sources`. Run `mvn spotless:apply`
  before committing. It formats Java with palantir-java-format, sorts the POM, and also reformats
  Markdown, including this file and the README.
- The build compiles with `mojo.java.target` 17.
- The modules compile against `mavenVersion` (3.9.16) and `resolverVersion` (1.9.27). The root POM
  sets `minimalMavenBuildVersion` to 3.9.6, and `mrm-maven-plugin` declares a literal Maven 3.9.0
  as its `<prerequisites>`. Raising `mavenVersion` must not raise the prerequisite.
- Fields of the mojo classes are plugin parameters; renaming one changes the plugin's
  configuration API.

## Unit tests

- JUnit 5, under each module's `src/test/java`; `mrm-servlet` also uses Mockito through
  `@ExtendWith(MockitoExtension.class)`.
- `mrm-jupiter-extension` has its tests in `*IT` classes, which Failsafe runs in `verify`.

## Integration tests

- The plugin's tests are in `mrm-maven-plugin/src/it` and run with the `run-its` profile, which is
  declared in that module. Each directory there is a project that starts a mock repository with
  the plugin, and runs a second invoker build, from its own `src/it`, against the repository
  URL it passes in as `mrm.repository.url`. The nested projects take real artifacts from Maven
  Central through the mock repository, so a run needs network access. Run one with:

  ```shell
  mvn -B -P run-its verify -pl mrm-maven-plugin -am -DskipTests -Dinvoker.test=custom-base-path
  ```

  `-am` builds the other modules first, because the plugin depends on them. There are no
  `setup-*` projects.

- They run in parallel, one per CPU core (`invoker.parallelThreads` 1C), and every forked Maven
  gets `JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1 -XX:+UseSerialGC`. A test must not depend on
  another test's output or on running alone.

- Write `verify.groovy` as `assert` statements. Invoker fails a script whose last expression is
  anything other than `true` or `null`, so a trailing `list.each { }` fails the test; use a `for`
  loop. The paths in those scripts reach out of the nested clone, such as
  `../../local-repo-it`, so check them when you move a project.

- The settings files under `src/it` take the repository URL from the `@mrm.repository.url@`
  filter, which the running mock repository fills in.

## Continuous integration

- `.github/workflows/maven.yml` calls the shared `maven-verify.yml@v5` workflow from
  `apache/maven-gh-actions-shared`.
- Branches pushed to this repository build under the `push` event.

## Dependencies

- Dependabot updates are monthly and grouped. Pins in `.github/dependabot.yml`, each with its
  reason in a comment there: Maven core artifacts stay below 3.10, Jetty below 10, Maven Resolver
  below 2 (the resolver line has to match `mavenVersion`), and `plexus-xml` below 4.

## Pull requests

- Squash and rebase merges are enabled; merge commits are not. Release Drafter, through the shared
  `apache/maven-gh-actions-shared` workflow, builds the release notes from pull request titles
  and labels, so the title should describe the change for a user.

