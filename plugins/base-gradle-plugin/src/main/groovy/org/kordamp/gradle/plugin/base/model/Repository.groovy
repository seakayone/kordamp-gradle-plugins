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
import org.gradle.api.Action
import org.gradle.util.ConfigureUtil

import static org.kordamp.gradle.StringUtils.isBlank
import static org.kordamp.gradle.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.8.0
 */
@CompileStatic
@Canonical
@ToString(includeNames = true)
class Repository {
    String name
    String url
    final Credentials credentials = new Credentials()

    @Override
    String toString() {
        toMap().toString()
    }

    Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>([
            name: name,
            url : url
        ])

        if (!credentials.empty) {
            map.credentials = new LinkedHashMap<String, Object>([
                username: credentials.username,
                password: credentials.password
            ])
        }

        map
    }

    String getName() {
        isNotBlank(this.@name) ? this.@name : url
    }

    void credentials(Action<? super Credentials> action) {
        action.execute(credentials)
    }

    void credentials(@DelegatesTo(Credentials) Closure action) {
        ConfigureUtil.configure(action, credentials)
    }

    Repository copyOf() {
        Repository copy = new Repository()
        copyInto(copy)
        copy
    }

    void copyInto(Repository copy) {
        copy.name = name
        copy.url = url
        credentials.copyInto(copy.credentials)
    }

    static void merge(Repository o1, Repository o2) {
        o1.name = o1.name ?: o2?.name
        o1.url = o1.url ?: o2?.url
        o1.credentials.merge(o1.credentials, o2?.credentials)
    }

    boolean isEmpty() {
        isBlank(url)
    }
}
