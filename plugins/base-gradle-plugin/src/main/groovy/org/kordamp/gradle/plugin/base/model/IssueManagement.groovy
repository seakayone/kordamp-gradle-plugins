/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2019 Andres Almiray.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kordamp.gradle.plugin.base.model

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.ToString

import static org.kordamp.gradle.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.22.0
 */
@CompileStatic
@Canonical
@ToString(includeNames = true)
class IssueManagement {
    String system
    String url

    @Override
    String toString() {
        toMap().toString()
    }

    Map<String, Object> toMap() {
        new LinkedHashMap<String, Object>([
            system: system,
            url : url
        ])
    }

    IssueManagement copyOf() {
        IssueManagement copy = new IssueManagement()
        copyInto(copy)
        copy
    }

    void copyInto(IssueManagement copy) {
        copy.system = system
        copy.url = url
    }

    static void merge(IssueManagement o1, IssueManagement o2) {
        o1.system = o1.system ?: o2?.system
        o1.url = o1.url ?: o2?.url
    }

    boolean isEmpty() {
        isBlank(system) && isBlank(url)
    }
}
