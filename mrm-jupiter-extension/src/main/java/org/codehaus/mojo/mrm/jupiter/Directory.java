/*
 * Copyright 2026 Robert Scholte
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
package org.codehaus.mojo.mrm.jupiter;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A reference to a directory.
 * Its value intentionally does not support placeholders.
 * To move it outside of the project (e.g. temp directory or user home) provide a non-argument constructor implementation of PathResolver
 *
 * @since 2.0
 */
public @interface Directory {

    /**
     *
     * @return the class that will resolve the basePath
     *
     * @see UserHomePathResolver
     */
    Class<? extends PathResolver> baseBathResolver() default UserDirPathResolver.class;

    /**
     * This value must be relative and must give the same result after normalizing.
     *
     * @return the path
     * @see Paths#get(String, String...)
     * @see Path#resolve(String)
     */
    String value();
}
