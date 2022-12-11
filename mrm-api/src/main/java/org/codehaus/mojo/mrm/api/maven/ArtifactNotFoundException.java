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

/**
 * An exception that indicates that an artifact could not be found.
 *
 * @since 1.0
 */
public class ArtifactNotFoundException extends Exception {
    /**
     * Ensure consistent serialization.
     *
     * @since 1.0
     */
    private static final long serialVersionUID = 1L;

    /**
     * The artifact.
     *
     * @since 1.0
     */
    private final Artifact artifact;

    /**
     * Creates a new {@link ArtifactNotFoundException}.
     *
     * @param artifact the artifact that was not found.
     * @since 1.0
     */
    public ArtifactNotFoundException(Artifact artifact) {
        this(artifact.toString(), artifact, null);
    }

    /**
     * Creates a new {@link ArtifactNotFoundException}.
     *
     * @param artifact the artifact that was not found.
     * @param cause    the reason why it was not found (or <code>null</code> if there is no specific reason)
     * @since 1.0
     */
    public ArtifactNotFoundException(Artifact artifact, Throwable cause) {
        this(artifact.toString(), artifact, cause);
    }

    /**
     * Creates a new {@link ArtifactNotFoundException}.
     *
     * @param message  The message.
     * @param artifact the artifact that was not found.
     * @since 1.0
     */
    public ArtifactNotFoundException(String message, Artifact artifact) {
        this(message, artifact, null);
    }

    /**
     * Creates a new {@link ArtifactNotFoundException}.
     *
     * @param message  The message.
     * @param artifact the artifact that was not found.
     * @param cause    the reason why it was not found (or <code>null</code> if there is no specific reason)
     * @since 1.0
     */
    public ArtifactNotFoundException(String message, Artifact artifact, Throwable cause) {
        super(message, cause);
        this.artifact = artifact;
    }

    /**
     * Gets the artifact that does not exist.
     *
     * @return the artifact that does not exist.
     * @since 1.0
     */
    public Artifact getArtifact() {
        return artifact;
    }
}
