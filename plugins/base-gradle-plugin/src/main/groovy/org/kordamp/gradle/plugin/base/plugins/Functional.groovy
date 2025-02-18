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
package org.kordamp.gradle.plugin.base.plugins

import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * @author Andres Almiray
 * @since 0.14.0
 */
@CompileStatic
@Canonical
class Functional {
    static final String PLUGIN_ID = 'org.kordamp.gradle.functional-test'

    boolean logging = true
    boolean aggregate = true

    private boolean loggingSet = false
    private boolean aggregateSet = false

    protected final Testing test

    Functional(Testing test) {
        this.test = test
    }

    void setLogging(boolean logging) {
        this.logging = logging
        this.loggingSet = true
    }

    boolean isLoggingSet() {
        this.loggingSet
    }

    void setAggregate(boolean aggregate) {
        this.aggregate = aggregate
        this.aggregateSet = true
    }

    boolean isAggregateSet() {
        this.aggregateSet
    }

    @Override
    String toString() {
        toMap().toString()
    }

    Map<String, Object> toMap() {
        new LinkedHashMap<>([
            logging  : logging,
            aggregate: aggregate
        ])
    }

    void copyInto(Functional copy) {
        copy.@logging = logging
        copy.@loggingSet = loggingSet
        copy.@aggregate = aggregate
        copy.@aggregateSet = aggregateSet
    }

    static void merge(Functional o1, Functional o2) {
        boolean thisLogging = (boolean) (o1.loggingSet ? o1.logging : o2.logging)
        boolean superLogging = (boolean) (o1.test.loggingSet) ? o1.test.logging : o2.logging
        o1.setLogging(thisLogging ?: superLogging)

        boolean thisAggregate = (boolean) (o1.aggregateSet ? o1.aggregate : o2.aggregate)
        boolean superAggregate = (boolean) (o1.test.aggregateSet) ? o1.test.aggregate : o2.aggregate
        o1.setAggregate(thisAggregate ?: superAggregate)
    }
}
