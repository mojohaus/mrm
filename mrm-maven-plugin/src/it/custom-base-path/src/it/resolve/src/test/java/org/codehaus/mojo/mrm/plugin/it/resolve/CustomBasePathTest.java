package org.codehaus.mojo.mrm.plugin.it.resolve;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

public class CustomBasePathTest {

    @Test
    void customBasePath() {
        final String mrmUri = System.getProperty("mrm.repository.url");

        /* Make sure the repo ends with the base path we have set in mrm-maven-plugin/src/it/custom-base-path/pom.xml */
        Assertions.assertThat(mrmUri).endsWith("foo/bar");

        /* Try to download something and make sure the content is as expected */
        RestAssured
            .get(mrmUri + "/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.pom")
            .then()
            .statusCode(200)
            .body(Matchers.containsString("<artifactId>commons-lang3</artifactId>"), Matchers.containsString("<version>3.12.0</version>"));
    }
}
