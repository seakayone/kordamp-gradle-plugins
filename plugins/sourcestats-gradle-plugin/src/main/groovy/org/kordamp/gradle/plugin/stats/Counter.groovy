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
package org.kordamp.gradle.plugin.stats

import groovy.transform.CompileStatic

/**
 * @author Andres Almiray
 * @since 0.5.0
 */
@CompileStatic
interface Counter {
    String EMPTY = /^\s*$/
    String SLASH_SLASH = /^\s*\/\/.*/
    String SLASH_STAR_STAR_SLASH = /^(.*)\/\*(.*)\*\/(.*)$/

    int count(File file)
}
