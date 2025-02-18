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
package org.kordamp.gradle.plugin.scaladoc

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.BuildAdapter
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenArtifact
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.scala.ScalaDoc
import org.kordamp.gradle.plugin.AbstractKordampPlugin
import org.kordamp.gradle.plugin.base.BasePlugin
import org.kordamp.gradle.plugin.base.ProjectConfigurationExtension
import org.kordamp.gradle.plugin.javadoc.JavadocPlugin

import static org.kordamp.gradle.PluginUtils.resolveEffectiveConfig
import static org.kordamp.gradle.plugin.base.BasePlugin.isRootProject

/**
 * Configures {@code scaladoc} and {@code scaladocJar} tasks.
 *
 * @author Andres Almiray
 * @since 0.15.0
 */
@CompileStatic
class ScaladocPlugin extends AbstractKordampPlugin {
    static final String SCALADOC_TASK_NAME = 'scaladoc'
    static final String SCALADOC_JAR_TASK_NAME = 'scaladocJar'
    static final String AGGREGATE_SCALADOC_TASK_NAME = 'aggregateScaladoc'
    static final String AGGREGATE_SCALADOC_JAR_TASK_NAME = 'aggregateScaladocJar'

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
        if (!project.plugins.findPlugin(ScaladocPlugin)) {
            project.pluginManager.apply(ScaladocPlugin)
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
        setEnabled(effectiveConfig.docs.scaladoc.enabled)

        List<ScalaDoc> docTasks = []
        project.tasks.withType(ScalaDoc) { ScalaDoc t -> if (t.name != AGGREGATE_SCALADOC_TASK_NAME && t.enabled) docTasks << t }
        project.childProjects.values().each { Project p ->
            if (p in effectiveConfig.docs.scaladoc.excludedProjects()) return
            p.tasks.withType(ScalaDoc) { ScalaDoc t -> if (t.enabled) docTasks << t }
        }
        docTasks = docTasks.unique()

        if (docTasks) {
            TaskProvider<ScalaDoc> aggregateScaladoc = project.tasks.named(AGGREGATE_SCALADOC_TASK_NAME, ScalaDoc,
                new Action<ScalaDoc>() {
                    @Override
                    void execute(ScalaDoc t) {
                        t.enabled = effectiveConfig.docs.scaladoc.enabled
                        t.dependsOn docTasks
                        t.source docTasks.source
                        t.classpath = project.files(docTasks.classpath)
                    }
                })

            project.tasks.named(AGGREGATE_SCALADOC_JAR_TASK_NAME, Jar,
                new Action<Jar>() {
                    @Override
                    void execute(Jar t) {
                        t.enabled = effectiveConfig.docs.scaladoc.enabled
                        t.from aggregateScaladoc.get().destinationDir
                        t.archiveClassifier.set effectiveConfig.docs.scaladoc.replaceJavadoc ? 'javadoc' : 'scaladoc'
                        t.onlyIf { aggregateScaladoc.get().didWork }
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

        project.pluginManager.withPlugin('scala-base') {
            project.afterEvaluate {
                ProjectConfigurationExtension effectiveConfig = resolveEffectiveConfig(project)
                setEnabled(effectiveConfig.docs.scaladoc.enabled)

                TaskProvider<ScalaDoc> scaladoc = createScaladocTask(project)
                TaskProvider<Jar> scaladocJar = createScaladocJarTask(project, scaladoc)
                project.tasks.findByName(org.gradle.api.plugins.BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(scaladocJar)
            }
        }
    }

    private TaskProvider<ScalaDoc> createScaladocTask(Project project) {
        project.tasks.named(SCALADOC_TASK_NAME, ScalaDoc,
            new Action<ScalaDoc>() {
                @Override
                @CompileDynamic
                void execute(ScalaDoc t) {
                    ProjectConfigurationExtension effectiveConfig = resolveEffectiveConfig(project)
                    t.enabled = effectiveConfig.docs.scaladoc.enabled
                    t.dependsOn project.tasks.named('classes')
                    t.group = JavaBasePlugin.DOCUMENTATION_GROUP
                    t.description = 'Generates Scaladoc API documentation'
                    t.source project.sourceSets.main.allSource
                    t.destinationDir = project.file("${project.buildDir}/docs/scaladoc")
                    effectiveConfig.docs.scaladoc.applyTo(t)
                }
            })
    }

    private TaskProvider<Jar> createScaladocJarTask(Project project, TaskProvider<ScalaDoc> scaladoc) {
        ProjectConfigurationExtension effectiveConfig = resolveEffectiveConfig(project)

        TaskProvider<Jar> scaladocJarTask = project.tasks.register(SCALADOC_JAR_TASK_NAME, Jar,
            new Action<Jar>() {
                @Override
                void execute(Jar t) {
                    t.enabled = effectiveConfig.docs.scaladoc.enabled
                    t.dependsOn scaladoc
                    t.group = JavaBasePlugin.DOCUMENTATION_GROUP
                    t.description = 'An archive of the Scaladoc API docs'
                    t.archiveClassifier.set('scaladoc')
                    t.from scaladoc.get().destinationDir
                }
            })

        if (effectiveConfig.docs.scaladoc.replaceJavadoc) {
            scaladocJarTask.configure(new Action<Jar>() {
                @Override
                void execute(Jar t) {
                    t.archiveClassifier.set('javadoc')
                }
            })
            project.tasks.findByName(JavadocPlugin.JAVADOC_TASK_NAME)?.enabled = false
            project.tasks.findByName(JavadocPlugin.JAVADOC_JAR_TASK_NAME)?.enabled = false
        }

        if (project.pluginManager.hasPlugin('maven-publish')) {
            PublishingExtension publishing = project.extensions.findByType(PublishingExtension)
            MavenPublication mainPublication = (MavenPublication) publishing.publications.findByName('main')
            if (effectiveConfig.docs.scaladoc.replaceJavadoc) {
                MavenArtifact javadocJar = mainPublication.artifacts.find { it.classifier == 'javadoc' }
                mainPublication.artifacts.remove(javadocJar)
            }
            mainPublication.artifact(scaladocJarTask.get())
        }

        scaladocJarTask
    }

    private void createAggregateTasks(Project project) {
        TaskProvider<ScalaDoc> aggregateScaladoc = project.tasks.register(AGGREGATE_SCALADOC_TASK_NAME, ScalaDoc,
            new Action<ScalaDoc>() {
                @Override
                void execute(ScalaDoc t) {
                    ProjectConfigurationExtension effectiveConfig = resolveEffectiveConfig(t.project)
                    t.enabled = false
                    t.group = JavaBasePlugin.DOCUMENTATION_GROUP
                    t.description = 'Aggregates Scaladoc API docs for all projects.'
                    t.destinationDir = project.file("${project.buildDir}/docs/aggregate-scaladoc")
                    effectiveConfig.docs.scaladoc.applyTo(t)
                }
            })

        project.tasks.register(AGGREGATE_SCALADOC_JAR_TASK_NAME, Jar,
            new Action<Jar>() {
                @Override
                void execute(Jar t) {
                    t.dependsOn aggregateScaladoc
                    t.enabled = false
                    t.group = JavaBasePlugin.DOCUMENTATION_GROUP
                    t.description = 'An archive of the aggregate Scaladoc API docs'
                    t.archiveClassifier.set('scaladoc')
                }
            })
    }
}
