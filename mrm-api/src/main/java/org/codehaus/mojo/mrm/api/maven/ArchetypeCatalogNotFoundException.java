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
public class ArchetypeCatalogNotFoundException extends Exception {
    /**
     * Ensure consistent serialization.
     *
     * @since 1.0
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new {@link ArchetypeCatalogNotFoundException}.
     *
     * @since 1.0
     */
    public ArchetypeCatalogNotFoundException() {
        super();
    }

    /**
     * Creates a new {@link ArchetypeCatalogNotFoundException}.
     *
     * @param message The message.
     * @since 1.0
     */
    public ArchetypeCatalogNotFoundException(String message) {
        this(message, null);
    }

    /**
     * Creates a new {@link ArchetypeCatalogNotFoundException}.
     *
     * @param message The message.
     * @param cause   the reason why it was not found (or <code>null</code> if there is no specific reason)
     * @since 1.0
     */
    public ArchetypeCatalogNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
