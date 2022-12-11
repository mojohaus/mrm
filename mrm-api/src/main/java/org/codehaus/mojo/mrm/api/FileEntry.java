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

package org.codehaus.mojo.mrm.api;

import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link Entry} that corresponds to a file.
 *
 * @since 1.0
 */
public interface FileEntry extends Entry {
    /**
     * Returns the size in bytes of the entry.
     *
     * @return the length of the entry in bytes or <code>-1L</code> if the length cannot be determined.
     * @throws IOException if an I/O error occurs.
     * @since 1.0
     */
    long getSize() throws IOException;

    /**
     * Returns the contents of the entry.
     *
     * @return the contents of the entry as an {@link InputStream} (caller is responsible for closing).
     * @throws IOException if an I/O error occurs.
     * @since 1.0
     */
    InputStream getInputStream() throws IOException;
}
