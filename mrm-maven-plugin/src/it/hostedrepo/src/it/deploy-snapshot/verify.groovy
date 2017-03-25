assert new File( basedir, '../../hosted-repo/org/mojohaus/mrm/hostedrepo/its/deploy/maven-metadata.xml').exists()
assert new File( basedir, '../../hosted-repo/org/mojohaus/mrm/hostedrepo/its/deploy/1.0.0-SNAPSHOT/maven-metadata.xml').exists()
def dir = new File( basedir, '../../hosted-repo/org/mojohaus/mrm/hostedrepo/its/deploy/1.0.0-SNAPSHOT/' )

def path
dir.eachFileMatch(~/deploy-1\.0\.0-\d{8}\.\d{6}-1\.pom/) { path = it.absolutePath }
assert path != null : "Could not locate pom based on regular expression"
assert new File(path).exists()
assert new File(path + '.md5').exists()
assert new File(path + '.sha1').exists()