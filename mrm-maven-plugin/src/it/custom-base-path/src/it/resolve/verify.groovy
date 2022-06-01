// mrm-resolve test project depends on assertj, so it should appear in the test's local repo
assert new File( basedir, '../../local-repo-it/org/assertj/assertj-core/3.22.0/assertj-core-3.22.0.pom').exists()
// We have downloaded commons-lang3-3.12.0.pom via mrm.repository.url from CustomBasePathTest, so it should appear in the parent local repo
assert new File( basedir, '../../../../../local-repo/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.pom').exists()
