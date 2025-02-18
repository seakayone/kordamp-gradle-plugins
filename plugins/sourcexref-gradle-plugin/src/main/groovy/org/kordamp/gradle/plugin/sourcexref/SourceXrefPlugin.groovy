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
package org.kordamp.gradle.plugin.sourcexref

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.BuildAdapter
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.kordamp.gradle.plugin.AbstractKordampPlugin
import org.kordamp.gradle.plugin.base.BasePlugin
import org.kordamp.gradle.plugin.base.ProjectConfigurationExtension
import org.kordamp.gradle.plugin.base.plugins.SourceXref

import static org.kordamp.gradle.PluginUtils.resolveEffectiveConfig
import static org.kordamp.gradle.StringUtils.isNotBlank
import static org.kordamp.gradle.plugin.base.BasePlugin.isRootProject

/**
 * @author Andres Almiray
 * @since 0.7.0
 */
@CompileStatic
class SourceXrefPlugin extends AbstractKordampPlugin {
    static final String SOURCE_XREF_TASK_NAME = 'sourceXref'
    static final String AGGREGATE_SOURCE_XREF_TASK_NAME = 'aggregateSourceXref'

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
        if (!project.plugins.findPlugin(SourceXrefPlugin)) {
            project.pluginManager.apply(SourceXrefPlugin)
        }
    }

    private void configureProject(Project project) {
        if (hasBeenVisited(project)) {
            return
        }
        setVisited(project, true)

        BasePlugin.applyIfMissing(project)

        project.pluginManager.withPlugin('java-base') {
            project.afterEvaluate {
                ProjectConfigurationExtension effectiveConfig = resolveEffectiveConfig(project)

                if (effectiveConfig.docs.sourceXref.enabled) {
                    TaskProvider<JxrTask> xrefTask = configureSourceXrefTask(project)
                    if (xrefTask?.get()?.enabled) {
                        effectiveConfig.docs.sourceXref.projects() << project
                        effectiveConfig.docs.sourceXref.xrefTasks() << xrefTask
                    } else {
                        effectiveConfig.docs.sourceXref.enabled = false
                    }
                }
                setEnabled(effectiveConfig.docs.sourceXref.enabled)
            }
        }
    }

    private void configureRootProject(Project project) {
        TaskProvider<JxrTask> jxrTask = project.tasks.register(AGGREGATE_SOURCE_XREF_TASK_NAME, JxrTask,
            new Action<JxrTask>() {
                @Override
                void execute(JxrTask t) {
                    t.group = 'Documentation'
                    t.description = 'Generates an aggregate JXR report of the source code.'
                    t.outputDirectory = project.file("${project.buildDir}/docs/aggregate-source-xref")
                    t.enabled = false
                }
            })

        TaskProvider<Jar> jxrJarTask = project.tasks.register(AGGREGATE_SOURCE_XREF_TASK_NAME + 'Jar', Jar,
            new Action<Jar>() {
                @Override
                void execute(Jar t) {
                    t.dependsOn jxrTask
                    t.group = 'Documentation'
                    t.description = 'An archive of the JXR report the source code.'
                    t.archiveClassifier.set 'sources-jxr'
                    t.from jxrTask.get().outputDirectory
                    t.enabled = false
                }
            })

        project.gradle.addBuildListener(new BuildAdapter() {
            @Override
            void projectsEvaluated(Gradle gradle) {
                configureAggregateSourceXrefTask(project, jxrTask, jxrJarTask)
            }
        })
    }

    private TaskProvider<JxrTask> configureSourceXrefTask(Project project) {
        ProjectConfigurationExtension effectiveConfig = resolveEffectiveConfig(project)

        TaskProvider<JxrTask> jxrTask = project.tasks.register(SOURCE_XREF_TASK_NAME, JxrTask,
            new Action<JxrTask>() {
                @Override
                void execute(JxrTask t) {
                    t.dependsOn project.tasks.named('classes')
                    t.group = 'Documentation'
                    t.description = 'Generates a JXR report of the source code.'
                    t.outputDirectory = project.file("${project.buildDir}/docs/source-xref")
                    t.sourceDirs = resolveSrcDirs(project)
                }
            })

        configureTask(effectiveConfig.docs.sourceXref, jxrTask)

        project.tasks.register(SOURCE_XREF_TASK_NAME + 'Jar', Jar,
            new Action<Jar>() {
                @Override
                void execute(Jar t) {
                    t.dependsOn jxrTask
                    t.group = 'Documentation'
                    t.description = 'An archive of the JXR report the source code.'
                    t.archiveClassifier.set 'sources-jxr'
                    t.from jxrTask.get().outputDirectory
                }
            })

        jxrTask
    }

    private void configureAggregateSourceXrefTask(Project project, TaskProvider<JxrTask> jxrTask, TaskProvider<Jar> jxrJarTask) {
        ProjectConfigurationExtension effectiveConfig = resolveEffectiveConfig(project)

        Set<Project> projects = new LinkedHashSet<>()
        Set<TaskProvider<? extends Task>> xrefTasks = new LinkedHashSet<>()
        FileCollection srcdirs = project.files()

        project.childProjects.values().each {
            SourceXref e = resolveEffectiveConfig(it).docs.sourceXref
            if (!e.enabled || effectiveConfig.docs.sourceXref.excludedProjects().intersect(e.projects())) return
            projects.addAll(e.projects())
            xrefTasks.addAll(e.xrefTasks())
            srcdirs = project.files(srcdirs, e.xrefTasks().collect { ((JxrTask) it.get()).sourceDirs })
        }

        jxrTask.configure(new Action<JxrTask>() {
            @Override
            void execute(JxrTask t) {
                t.dependsOn xrefTasks
                t.sourceDirs = srcdirs
                t.enabled = effectiveConfig.docs.sourceXref.enabled
            }
        })
        configureTask(effectiveConfig.docs.sourceXref, jxrTask)

        jxrJarTask.configure(new Action<Jar>() {
            @Override
            void execute(Jar t) {
                t.enabled = effectiveConfig.docs.sourceXref.enabled
            }
        })
    }

    private TaskProvider<JxrTask> configureTask(SourceXref sourceXref, TaskProvider<JxrTask> jxrTask) {
        jxrTask.configure(new Action<JxrTask>() {
            @Override
            void execute(JxrTask t) {
                if (isNotBlank(sourceXref.templateDir)) t.templateDir = sourceXref.templateDir
                if (isNotBlank(sourceXref.inputEncoding)) t.inputEncoding = sourceXref.inputEncoding
                if (isNotBlank(sourceXref.outputEncoding)) t.outputEncoding = sourceXref.outputEncoding
                if (isNotBlank(sourceXref.windowTitle)) t.windowTitle = sourceXref.windowTitle
                if (isNotBlank(sourceXref.docTitle)) t.docTitle = sourceXref.docTitle
                if (isNotBlank(sourceXref.bottom)) t.bottom = sourceXref.bottom
                if (isNotBlank(sourceXref.stylesheet)) t.stylesheet = sourceXref.stylesheet
                if (sourceXref.javaVersion) t.javaVersion = sourceXref.javaVersion
                if (sourceXref.excludes) t.excludes.addAll(sourceXref.excludes)
                if (sourceXref.includes) t.includes.addAll(sourceXref.includes)
            }
        })


        jxrTask
    }

    private boolean hasJavaPlugin(Project project) {
        project.pluginManager.hasPlugin('java-base')
    }

    private boolean hasGroovyPlugin(Project project) {
        project.pluginManager.hasPlugin('groovy-base')
    }

    @CompileDynamic
    private FileCollection resolveSrcDirs(Project project) {
        try {
            if (project.sourceSets.main) {
                if (hasGroovyPlugin(project)) {
                    return project.files(project.files(
                        project.sourceSets.main.groovy.srcDirs,
                        project.sourceSets.main.java.srcDirs).files.findAll { file ->
                        file.exists()
                    })
                } else if (hasJavaPlugin(project)) {
                    return project.files(
                        project.files(project.sourceSets.main.java.srcDirs).files.findAll { file ->
                            file.exists()
                        })
                }
            }
        } catch (Exception ignored) {
            // ignore this project
            return project.files()
        }
    }
}
