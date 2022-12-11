/*
 * Copyright 2011 Stephen Connolly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.mojo.mrm.api.maven;

import java.io.Serializable;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Represents a specific artifact in a Maven repository. Implements {@link Comparable} to sort based on
 * {@link #getGroupId()} and then {@link #getName()}.
 *
 * @serial
 * @since 1.0
 */
public final class Artifact implements Serializable, Comparable<Artifact> {

    /**
     * Ensure consistent serialization.
     *
     * @since 1.0
     */
    private static final long serialVersionUID = 1L;

    /**
     * The groupId of the artifact.
     *
     * @since 1.0
     */
    private final String groupId;

    /**
     * The artifactId of the artifact.
     *
     * @since 1.0
     */
    private final String artifactId;

    /**
     * The version of the artifact.
     *
     * @since 1.0
     */
    private final String version;

    /**
     * The type of the artifact.
     *
     * @since 1.0
     */
    private final String type;

    /**
     * The classifier of the artifact or <code>null</code> if the artifact does not have a classifier.
     *
     * @since 1.0
     */
    private final String classifier;

    /**
     * The timestamp of the artifact or <code>null</code> if the version is either a release version or a
     * non-timestamped SNAPSHOT.
     *
     * @since 1.0
     */
    private final Long timestamp;

    /**
     * The build number of the artifact or <code>null</code> if the version is either a release version or a
     * non-timestamped SNAPSHOT.
     *
     * @since 1.0
     */
    private final Integer buildNumber;

    /**
     * The lazy idempotent cache of the artifact's name.
     *
     * @since 1.0
     */
    private transient String name;

    /**
     * The lazy idempotent cache of the artifact's timestamp version string (which will be equal to the {@link #version}
     * for either a release version or a non-timestamped SNAPSHOT.
     *
     * @since 1.0
     */
    private transient String timestampVersion;

    /**
     * Common internal constructor.
     *
     * @param groupId     The groupId.
     * @param artifactId  The artifactId.
     * @param version     The version.
     * @param classifier  The classifier (or <code>null</code>).
     * @param type        The type.
     * @param timestamp   The timestamp (or <code>null</code>).
     * @param buildNumber The build number (or <code>null</code>, however must not be <code>null</code> if
     *                    <code>timestamp!=null</code>)
     * @since 1.0
     */
    private Artifact(
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String type,
            Long timestamp,
            Integer buildNumber) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.classifier = classifier;
        this.timestamp = isSnapshot() ? timestamp : null;
        this.buildNumber = isSnapshot() ? buildNumber : null;
    }

    /**
     * A timestamped classified snapshot artifact.
     *
     * @param groupId     The groupId.
     * @param artifactId  The artifactId.
     * @param version     The version.
     * @param classifier  The classifier (or <code>null</code>).
     * @param type        The type.
     * @param timestamp   The timestamp.
     * @param buildNumber The build number.
     * @since 1.0
     */
    public Artifact(
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String type,
            long timestamp,
            int buildNumber) {
        this(groupId, artifactId, version, classifier, type, Long.valueOf(timestamp), Integer.valueOf(buildNumber));
    }

    /**
     * A timestamped snapshot artifact.
     *
     * @param groupId     The groupId.
     * @param artifactId  The artifactId.
     * @param version     The version.
     * @param type        The type.
     * @param timestamp   The timestamp.
     * @param buildNumber The build number.
     * @since 1.0
     */
    public Artifact(String groupId, String artifactId, String version, String type, long timestamp, int buildNumber) {
        this(groupId, artifactId, version, null, type, new Long(timestamp), new Integer(buildNumber));
    }

    /**
     * A classified snapshot artifact.
     *
     * @param groupId    The groupId.
     * @param artifactId The artifactId.
     * @param version    The version.
     * @param classifier The classifier (or <code>null</code>).
     * @param type       The type.
     * @since 1.0
     */
    public Artifact(String groupId, String artifactId, String version, String classifier, String type) {
        this(groupId, artifactId, version, classifier, type, null, null);
    }

    /**
     * An artifact.
     *
     * @param groupId    The groupId.
     * @param artifactId The artifactId.
     * @param version    The version.
     * @param type       The type.
     * @since 1.0
     */
    public Artifact(String groupId, String artifactId, String version, String type) {
        this(groupId, artifactId, version, null, type);
    }

    /**
     * Returns the name of the artifact.
     *
     * @return the name of the artifact.
     * @since 1.0
     */
    public String getName() {
        if (name == null) {
            name = MessageFormat.format(
                    "{0}-{1}{2}.{3}",
                    new Object[] {artifactId, getTimestampVersion(), (classifier == null ? "" : "-" + classifier), type
                    });
        }
        return name;
    }

    /**
     * Returns the groupId of the artifact.
     *
     * @return the groupId of the artifact.
     * @since 1.0
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Returns the artifactId of the artifact.
     *
     * @return the artifactId of the artifact.
     * @since 1.0
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Returns the version of the artifact.
     *
     * @return the version of the artifact.
     * @since 1.0
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the type of the artifact.
     *
     * @return the type of the artifact.
     * @since 1.0
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the classifier of the artifact (may be <code>null</code>).
     *
     * @return the classifier of the artifact (may be <code>null</code>).
     * @since 1.0
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * Returns the timestamp of the artifact (may be <code>null</code>).
     *
     * @return the timestamp of the artifact (may be <code>null</code>).
     * @since 1.0
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the build number of the artifact (may be <code>null</code>).
     *
     * @return the build number of the artifact (may be <code>null</code>).
     * @since 1.0
     */
    public Integer getBuildNumber() {
        return buildNumber;
    }

    /**
     * Returns the timestamp (formatted as a <code>yyyyMMdd.HHmmss</code> string) of the artifact
     * (may be <code>null</code>).
     *
     * @return the timestamp (formatted as a <code>yyyyMMdd.HHmmss</code> string) of the artifact
     * (may be <code>null</code>).
     * @since 1.0
     */
    public String getTimestampString() {
        if (timestamp == null) {
            return null;
        } else {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd.HHmmss");
            fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            return fmt.format(new Date(timestamp.longValue()));
        }
    }

    /**
     * Returns the timestamp version. <ul>
     * <li>For release artifacts, this will be the same as {@link #getVersion()}.</li>
     * <li>For non-timestamped SNAPSHOTS, this will be the same as {@link #getVersion()}.</li>
     * <li>For timestamped SNAPSHOTS, this will be the timestamp version, i.e. the {@link #getVersion()} with
     * <code>SNAPSHOT</code> replaced by {@link #getTimestampString()} and the {@link #getBuildNumber()} separated
     * by a <code>-</code>.</li>
     * </ul>
     *
     * @return the timestamp version.
     * @since 1.0
     */
    public String getTimestampVersion() {
        if (timestampVersion == null) {
            if (timestamp != null) {
                assert isSnapshot();
                SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd.HHmmss");
                fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                timestampVersion = MessageFormat.format(
                        "{0}-{1}-{2}",
                        new Object[] {
                            this.version.substring(0, this.version.length() - "-SNAPSHOT".length()),
                            fmt.format(new Date(timestamp.longValue())),
                            buildNumber
                        });
            } else {
                timestampVersion = version;
            }
        }
        return timestampVersion;
    }

    /**
     * Returns <code>true</code> if and only if the artifact is a SNAPSHOT artifact.
     *
     * @return <code>true</code> if and only if the artifact is a SNAPSHOT artifact.
     * @since 1.0
     */
    public boolean isSnapshot() {
        return version.endsWith("-SNAPSHOT");
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Artifact artifact = (Artifact) o;

        if (!groupId.equals(artifact.groupId)) {
            return false;
        }
        if (!artifactId.equals(artifact.artifactId)) {
            return false;
        }
        if (!version.equals(artifact.version)) {
            return false;
        }
        if (!type.equals(artifact.type)) {
            return false;
        }
        if (!Objects.equals(classifier, artifact.classifier)) {
            return false;
        }
        if (!Objects.equals(buildNumber, artifact.buildNumber)) {
            return false;
        }
        if (!Objects.equals(timestamp, artifact.timestamp)) {
            return false;
        }

        return true;
    }

    /**
     * More lax version of {@link #equals(Object)} that matches SNAPSHOTs with their corresponding timestamped versions.
     *
     * @param artifact the artifact to compare with.
     * @return <code>true</code> if this artifact is the same as the specified artifact (where timestamps are ignored
     * for SNAPSHOT versions).
     * @since 1.0
     */
    public boolean equalSnapshots(Artifact artifact) {
        if (this == artifact) {
            return true;
        }

        if (!groupId.equals(artifact.groupId)) {
            return false;
        }
        if (!artifactId.equals(artifact.artifactId)) {
            return false;
        }
        if (!version.equals(artifact.version)) {
            return false;
        }
        if (!type.equals(artifact.type)) {
            return false;
        }
        if (!Objects.equals(classifier, artifact.classifier)) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("Artifact");
        sb.append("{").append(groupId);
        sb.append(":").append(artifactId);
        sb.append(":").append(getTimestampVersion());
        if (classifier != null) {
            sb.append(":").append(classifier);
        }
        sb.append(":").append(type);
        sb.append('}');
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(Artifact that) {
        int rv = this.getGroupId().compareTo(that.getGroupId());
        return rv == 0 ? getName().compareTo(that.getName()) : rv;
    }
}
