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
import org.gradle.api.Project
import org.kordamp.gradle.CollectionUtils
import org.kordamp.gradle.plugin.base.ProjectConfigurationExtension

import static org.kordamp.gradle.StringUtils.getNaturalName
import static org.kordamp.gradle.StringUtils.getPropertyNameForLowerCaseHyphenSeparatedName

/**
 * @author Andres Almiray
 * @since 0.8.0
 */
@CompileStatic
@Canonical
class Stats extends AbstractFeature {
    static final String XML = 'xml'
    static final String HTML = 'html'
    static final String TXT = 'txt'

    Map<String, String> counters = [:]
    Map<String, Map<String, String>> paths = [:]
    List<String> formats = ['xml', 'txt']

    Stats(ProjectConfigurationExtension config, Project project) {
        super(config, project)
        paths.putAll(defaultPaths())
    }

    @Override
    String toString() {
        toMap().toString()
    }

    @Override
    Map<String, Map<String, Object>> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>(enabled: enabled)

        if (enabled) {
            map.formats = formats
            map.counters = counters
            map.paths = paths
        }

        new LinkedHashMap<>('stats': map)
    }

    static Map<String, Map<String, String>> defaultPaths() {
        Map<String, Map<String, String>> basePaths = [:]

        [
            java      : 'Java',
            groovy    : 'Groovy',
            scala     : 'Scala',
            kt        : 'Kotlin',
            kts       : 'Kotlin Script',
            js        : 'Javascript',
            css       : 'CSS',
            scss      : 'SASS',
            xml       : 'XML',
            html      : 'HTML',
            fxml      : 'FXML',
            properties: 'Properties',
            sql       : 'SQL',
            yaml      : 'YAML',
            clojure   : 'Clojure'
        ].each { extension, name ->
            ['test', 'integration-test', 'functional-test'].each { source ->
                String classifier = getPropertyNameForLowerCaseHyphenSeparatedName(source)
                basePaths[classifier + extension.capitalize()] = [name: name + ' ' + getNaturalName(classifier) + ' Sources', path: 'src/' + source, extension: extension]
            }
        }

        basePaths.java = [name: 'Java Sources', path: '.*', extension: 'java']
        basePaths.groovy = [name: 'Groovy Sources', path: '.*', extension: 'groovy']
        basePaths.scala = [name: 'Scala Sources', path: '.*', extension: 'scala']
        basePaths.kt = [name: 'Kotlin Sources', path: '.*', extension: 'kt']
        basePaths.kts = [name: 'Kotlin Scripts', path: '.*', extension: 'kts']
        basePaths.js = [name: 'Javascript Sources', path: '.*', extension: 'js']
        basePaths.css = [name: 'CSS Sources', path: '.*', extension: 'css']
        basePaths.scss = [name: 'SASS Sources', path: '.*', extension: 'scss']
        basePaths.xml = [name: 'XML Sources', path: '.*', extension: 'xml']
        basePaths.html = [name: 'HTML Sources', path: '.*', extension: 'html']
        basePaths.fxml = [name: 'FXML Sources', path: '.*', extension: 'fxml']
        basePaths.properties = [name: 'Properties', path: '.*', extension: 'properties']
        basePaths.sql = [name: 'SQL', path: '.*', extension: 'sql']
        basePaths.yaml = [name: 'Yaml', path: '.*', extension: 'yaml']
        basePaths.clj = [name: 'Clojure', path: '.*', extension: 'clj']

        basePaths
    }

    void copyInto(Stats copy) {
        super.copyInto(copy)
        copy.counters.putAll(counters)
        copy.paths.putAll(paths)
        List<String> fmts = new ArrayList<>(copy.formats)
        copy.formats.clear()
        copy.formats.addAll((fmts + formats).unique())
        copy.formats.addAll(formats)
    }

    static void merge(Stats o1, Stats o2) {
        AbstractFeature.merge(o1, o2)
        o1.counters.putAll(o2.counters)
        o1.paths.putAll(o2.paths)
        CollectionUtils.merge(o1.formats, o2?.formats)
    }
}
