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
package org.kordamp.gradle.plugin.javadoc

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.BuildAdapter
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.kordamp.gradle.plugin.AbstractKordampPlugin
import org.kordamp.gradle.plugin.base.BasePlugin
import org.kordamp.gradle.plugin.base.ProjectConfigurationExtension

import static org.kordamp.gradle.PluginUtils.resolveEffectiveConfig
import static org.kordamp.gradle.plugin.base.BasePlugin.isRootProject

/**
 * Configures {@code javadoc} and {@code javadocJar} tasks.
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class JavadocPlugin extends AbstractKordampPlugin {
    static final String JAVADOC_TASK_NAME = 'javadoc'
    static final String JAVADOC_JAR_TASK_NAME = 'javadocJar'
    static final String AGGREGATE_JAVADOC_TASK_NAME = 'aggregateJavadoc'
    static final String AGGREGATE_JAVADOC_JAR_TASK_NAME = 'aggregateJavadocJar'

    Project project

    void apply(Project project) {
        this.project = project

        configureProject(project)
        if (isRootProject(project)) {
            configureRootProject(project)
            project.childProjects.values().each {
                configureProject(it)
            }
        }
    }

    static void applyIfMissing(Project project) {
        if (!project.plugins.findPlugin(JavadocPlugin)) {
            project.pluginManager.apply(JavadocPlugin)
        }
    }

    private void configureRootProject(Project project) {
        createAggregateTasks(project)

        project.gradle.addBuildListener(new BuildAdapter() {
            @Override
            void projectsEvaluated(Gradle gradle) {
                doConfigureRootProject(project)
            }
        })
    }

    private void doConfigureRootProject(Project project) {
        ProjectConfigurationExtension effectiveConfig = resolveEffectiveConfig(project)
        setEnabled(effectiveConfig.docs.javadoc.enabled)

        List<Javadoc> docTasks = []
        project.tasks.withType(Javadoc) { Javadoc t -> if (t.name != AGGREGATE_JAVADOC_TASK_NAME && t.enabled) docTasks << t }
        project.childProjects.values().each { Project p ->
            p.tasks.withType(Javadoc) { Javadoc t -> if (t.enabled) docTasks << t }
        }
        docTasks = docTasks.unique()

        if (docTasks) {
            TaskProvider<Javadoc> aggregateJavadoc = project.tasks.named(AGGREGATE_JAVADOC_TASK_NAME, Javadoc,
                new Action<Javadoc>() {
                    @Override
                    void execute(Javadoc t) {
                        t.enabled = effectiveConfig.docs.javadoc.enabled
                        t.dependsOn docTasks
                        t.source docTasks.source
                        t.classpath = project.files(docTasks.classpath)
                    }
                })

            project.tasks.named(AGGREGATE_JAVADOC_JAR_TASK_NAME, Jar,
                new Action<Jar>() {
                    @Override
                    void execute(Jar t) {
                        t.enabled = effectiveConfig.docs.javadoc.enabled
                        t.from aggregateJavadoc.get().destinationDir
                        t.onlyIf { aggregateJavadoc.get().didWork }
                    }
                })
        }
    }

    private void configureProject(Project project) {
        if (hasBeenVisited(project)) {
            return
        }
        setVisited(project, true)

        BasePlugin.applyIfMissing(project)

        project.pluginManager.withPlugin('java-base', new Action<AppliedPlugin>() {
            @Override
            void execute(AppliedPlugin appliedPlugin) {
                project.tasks.register('checkAutoLinks', CheckAutoLinksTask.class,
                    new Action<CheckAutoLinksTask>() {
                        void execute(CheckAutoLinksTask t) {
                            t.group = 'Documentation'
                            t.description = 'Checks if generated Javadoc auto links are reachable.'
                        }
                    })

                project.afterEvaluate {
                    ProjectConfigurationExtension effectiveConfig = resolveEffectiveConfig(project)
                    setEnabled(effectiveConfig.docs.javadoc.enabled)

                    TaskProvider<Javadoc> javadoc = createJavadocTask(project)
                    TaskProvider<Jar> javadocJar = createJavadocJarTask(project, javadoc)
                    project.tasks.findByName(org.gradle.api.plugins.BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(javadocJar)
                }
            }
        })
    }

    private TaskProvider<Javadoc> createJavadocTask(Project project) {
        project.tasks.named(JAVADOC_TASK_NAME, Javadoc,
            new Action<Javadoc>() {
                @Override
                @CompileDynamic
                void execute(Javadoc t) {
                    ProjectConfigurationExtension effectiveConfig = resolveEffectiveConfig(t.project)
                    t.enabled = effectiveConfig.docs.javadoc.enabled
                    t.dependsOn project.tasks.named('classes')
                    t.group = JavaBasePlugin.DOCUMENTATION_GROUP
                    t.description = 'Generates Javadoc API documentation'
                    t.destinationDir = project.file("${project.buildDir}/docs/javadoc")
                    t.source project.sourceSets.main.allJava
                    effectiveConfig.docs.javadoc.applyTo(t)
                    t.options.footer = "Copyright &copy; ${effectiveConfig.info.copyrightYear} ${effectiveConfig.info.getAuthors().join(', ')}. All rights reserved."
                    if (JavaVersion.current().isJava8Compatible()) {
                        t.options.addBooleanOption('Xdoclint:none', true)
                        t.options.quiet()
                    }
                }
            })
    }

    private TaskProvider<Jar> createJavadocJarTask(Project project, TaskProvider<Javadoc> javadoc) {
        project.tasks.register(JAVADOC_JAR_TASK_NAME, Jar,
            new Action<Jar>() {
                @Override
                void execute(Jar t) {
                    t.enabled = resolveEffectiveConfig(t.project).docs.javadoc.enabled
                    t.group = JavaBasePlugin.DOCUMENTATION_GROUP
                    t.description = 'An archive of the Javadoc API docs'
                    t.archiveClassifier.set('javadoc')
                    t.dependsOn javadoc
                    t.from javadoc.get().destinationDir
                }
            })
    }

    private void createAggregateTasks(Project project) {
        TaskProvider<Javadoc> aggregateJavadoc = project.tasks.register(AGGREGATE_JAVADOC_TASK_NAME, Javadoc,
            new Action<Javadoc>() {
                @Override
                @CompileDynamic
                void execute(Javadoc t) {
                    ProjectConfigurationExtension effectiveConfig = resolveEffectiveConfig(t.project)
                    t.enabled = false
                    t.group = JavaBasePlugin.DOCUMENTATION_GROUP
                    t.description = 'Aggregates Javadoc API docs for all projects.'
                    t.destinationDir = project.file("${project.buildDir}/docs/aggregate-javadoc")
                    effectiveConfig.docs.javadoc.applyTo(t)
                    t.options.footer = "Copyright &copy; ${effectiveConfig.info.copyrightYear} ${effectiveConfig.info.getAuthors().join(', ')}. All rights reserved."
                    if (JavaVersion.current().isJava8Compatible()) {
                        t.options.addBooleanOption('Xdoclint:none', true)
                        t.options.quiet()
                    }
                }
            })

        project.tasks.register(AGGREGATE_JAVADOC_JAR_TASK_NAME, Jar,
            new Action<Jar>() {
                @Override
                void execute(Jar t) {
                    t.enabled = false
                    t.dependsOn aggregateJavadoc
                    t.group = JavaBasePlugin.DOCUMENTATION_GROUP
                    t.description = 'An archive of the aggregate Javadoc API docs'
                    t.archiveClassifier.set('javadoc')
                }
            })
    }
}
