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

import java.lang.annotation.Retention;

import org.codehaus.mojo.mrm.impl.transform.TransformDirectiveSourceFactory;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation based representation of {@link org.codehaus.mojo.mrm.plugin.MockRepo}
 * @since 2.0.0
 */
@Retention(RUNTIME)
public @interface MockRepo {

    /**
     * Path to the directory containing the mock repository content.
     *
     * @return path to the mock repository source directory
     */
    Directory source();

    /**
     * If set, the {@link #source()} directory will be cloned to this path before the server starts.
     * This is useful in case additional files are generated.
     * Relative paths are resolved against the result of baseBathResolverm which defaults to the working directory.
     *
     * @return path to the clone target directory, or empty string to skip cloning
     */
    Directory cloneTo() default @Directory(value = "", baseBathResolver = SourcePathResolver.class);

    /**
     * When {@link #cloneTo()} is set, controls whether the clone target directory is cleaned
     * before copying. Defaults to {@code false}.
     *
     * @return {@code true} to clean the clone target before each run
     */
    boolean cloneClean() default false;

    /**
     * Controls whether directory content is archived lazily (on first access) or eagerly at startup.
     * Defaults to {@code true} (lazy).
     *
     * @return {@code true} to archive directory content on demand
     */
    boolean lazyArchiver() default true;

    Class<? extends TransformDirectiveSourceFactory> transformDirectiveSource() default
            NoOpTransformDirectiveSourceFactory.class;
}
