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
package org.kordamp.gradle.plugin.base

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.tasks.JavaExec
import org.kordamp.gradle.PluginUtils
import org.kordamp.gradle.plugin.AbstractKordampPlugin
import org.kordamp.gradle.plugin.base.tasks.ConfigurationSettingsTask
import org.kordamp.gradle.plugin.base.tasks.ConfigurationsTask
import org.kordamp.gradle.plugin.base.tasks.EffectiveSettingsTask
import org.kordamp.gradle.plugin.base.tasks.ExtensionsTask
import org.kordamp.gradle.plugin.base.tasks.GroovyCompilerSettingsTask
import org.kordamp.gradle.plugin.base.tasks.JarSettingsTask
import org.kordamp.gradle.plugin.base.tasks.JavaCompilerSettingsTask
import org.kordamp.gradle.plugin.base.tasks.JavaExecSettingsTask
import org.kordamp.gradle.plugin.base.tasks.KotlinCompilerSettingsTask
import org.kordamp.gradle.plugin.base.tasks.ListIncludedBuildsTask
import org.kordamp.gradle.plugin.base.tasks.ListProjectsTask
import org.kordamp.gradle.plugin.base.tasks.PluginsTask
import org.kordamp.gradle.plugin.base.tasks.ProjectPropertiesTask
import org.kordamp.gradle.plugin.base.tasks.RepositoriesTask
import org.kordamp.gradle.plugin.base.tasks.ScalaCompilerSettingsTask
import org.kordamp.gradle.plugin.base.tasks.SourceSetSettingsTask
import org.kordamp.gradle.plugin.base.tasks.SourceSetsTask
import org.kordamp.gradle.plugin.base.tasks.TarSettingsTask
import org.kordamp.gradle.plugin.base.tasks.TaskSettingsTask
import org.kordamp.gradle.plugin.base.tasks.TestSettingsTask
import org.kordamp.gradle.plugin.base.tasks.WarSettingsTask
import org.kordamp.gradle.plugin.base.tasks.ZipSettingsTask

import static org.kordamp.gradle.PluginUtils.isAndroidProject

/**
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class BasePlugin extends AbstractKordampPlugin {
    static final String ORG_KORDAMP_GRADLE_BASE_VALIDATE = 'org.kordamp.gradle.base.validate'

    Project project

    void apply(Project project) {
        this.project = project

        if (hasBeenVisited(project)) {
            return
        }
        setVisited(project, true)

        if (!project.plugins.findPlugin(org.gradle.api.plugins.BasePlugin)) {
            project.pluginManager.apply(org.gradle.api.plugins.BasePlugin)
        }

        if (!project.extensions.findByType(ProjectConfigurationExtension)) {
            project.extensions.create(ProjectConfigurationExtension.CONFIG_NAME, ProjectConfigurationExtension, project)
        }

        project.tasks.register('effectiveSettings', EffectiveSettingsTask,
            new Action<EffectiveSettingsTask>() {
                @Override
                void execute(EffectiveSettingsTask t) {
                    t.group = 'Insight'
                    t.description = "Displays resolved settings for project '$project.name'."
                }
            })

        project.tasks.register('repositories', RepositoriesTask,
            new Action<RepositoriesTask>() {
                @Override
                void execute(RepositoriesTask t) {
                    t.group = 'Insight'
                    t.description = "Displays all repositories for project '$project.name'."
                }
            })

        project.tasks.register('plugins', PluginsTask,
            new Action<PluginsTask>() {
                @Override
                void execute(PluginsTask t) {
                    t.group = 'Insight'
                    t.description = "Displays all plugins applied to project '$project.name'."
                }
            })

        project.tasks.register('extensions', ExtensionsTask,
            new Action<ExtensionsTask>() {
                @Override
                void execute(ExtensionsTask t) {
                    t.group = 'Insight'
                    t.description = "Displays all extensions applied to project '$project.name'."
                }
            })

        project.tasks.register('configurations', ConfigurationsTask,
            new Action<ConfigurationsTask>() {
                @Override
                void execute(ConfigurationsTask t) {
                    t.group = 'Insight'
                    t.description = "Displays all configurations available in project '$project.name'."
                }
            })

        project.tasks.register('projectProperties', ProjectPropertiesTask,
            new Action<ProjectPropertiesTask>() {
                @Override
                void execute(ProjectPropertiesTask t) {
                    t.group = 'Insight'
                    t.description = "Displays all properties found in project '$project.name'."
                }
            })

        project.tasks.register('configurationSettings', ConfigurationSettingsTask,
            new Action<ConfigurationSettingsTask>() {
                @Override
                void execute(ConfigurationSettingsTask t) {
                    t.group = 'Insight'
                    t.description = 'Display the settings of a Configuration.'
                }
            })

        project.tasks.addRule('Pattern: <ConfigurationName>ConfigurationSettings: Displays the settings of a Configuration.', new Action<String>() {
            @Override
            void execute(String configurationName) {
                if (configurationName.endsWith('ConfigurationSettings')) {
                    String resolvedConfigurationName = configurationName - 'ConfigurationSettings'
                    project.tasks.register(configurationName, ConfigurationSettingsTask,
                        new Action<ConfigurationSettingsTask>() {
                            @Override
                            void execute(ConfigurationSettingsTask t) {
                                t.group = 'Insight'
                                t.configuration = resolvedConfigurationName
                                t.description = "Display the settings of the '${resolvedConfigurationName}' Configuration."
                            }
                        })
                }
            }
        })

        project.tasks.register('zipSettings', ZipSettingsTask,
                new Action<ZipSettingsTask>() {
                    @Override
                    void execute(ZipSettingsTask t) {
                        t.group = 'Insight'
                        t.description = 'Display ZIP settings.'
                    }
                })

        project.tasks.addRule('Pattern: <ZipName>ZipSettings: Displays settings of a ZIP task.', new Action<String>() {
            @Override
            void execute(String taskName) {
                if (taskName.endsWith('ZipSettings')) {
                    String resolvedTaskName = taskName - 'ZipSettings'
                    resolvedTaskName = resolvedTaskName ?: 'zip'
                    project.tasks.register(taskName, ZipSettingsTask,
                            new Action<ZipSettingsTask>() {
                                @Override
                                void execute(ZipSettingsTask t) {
                                    t.group = 'Insight'
                                    t.task = resolvedTaskName
                                    t.description = "Display settings of the '${resolvedTaskName}' ZIP task."
                                }
                            })
                }
            }
        })

        project.tasks.register('tarSettings', TarSettingsTask,
                new Action<TarSettingsTask>() {
                    @Override
                    void execute(TarSettingsTask t) {
                        t.group = 'Insight'
                        t.description = 'Display TAR settings.'
                    }
                })

        project.tasks.addRule('Pattern: <TarName>TarSettings: Displays settings of a TAR task.', new Action<String>() {
            @Override
            void execute(String taskName) {
                if (taskName.endsWith('TarSettings')) {
                    String resolvedTaskName = taskName - 'TarSettings'
                    resolvedTaskName = resolvedTaskName ?: 'tar'
                    project.tasks.register(taskName, TarSettingsTask,
                            new Action<TarSettingsTask>() {
                                @Override
                                void execute(TarSettingsTask t) {
                                    t.group = 'Insight'
                                    t.task = resolvedTaskName
                                    t.description = "Display settings of the '${resolvedTaskName}' TAR task."
                                }
                            })
                }
            }
        })

        project.tasks.register('taskSettings', TaskSettingsTask,
                new Action<TaskSettingsTask>() {
                    @Override
                    void execute(TaskSettingsTask t) {
                        t.group = 'Insight'
                        t.description = 'Display the settings of a Task.'
                    }
                })

        project.tasks.addRule('Pattern: <TaskName>TaskSettings: Displays the settings of a Task.', new Action<String>() {
            @Override
            void execute(String taskName) {
                if (taskName.endsWith('TaskSettings')) {
                    String resolvedTaskName = taskName - 'TaskSettings'
                    project.tasks.register(taskName, TaskSettingsTask,
                            new Action<TaskSettingsTask>() {
                                @Override
                                void execute(TaskSettingsTask t) {
                                    t.group = 'Insight'
                                    t.task = resolvedTaskName
                                    t.description = "Display the settings of the '${resolvedTaskName}' Task."
                                }
                            })
                }
            }
        })

        project.pluginManager.withPlugin('java-base', new Action<AppliedPlugin>() {
            @Override
            void execute(AppliedPlugin appliedPlugin) {
                if (!isAndroidProject(project)) {
                    project.tasks.register('sourceSets', SourceSetsTask,
                        new Action<SourceSetsTask>() {
                            @Override
                            void execute(SourceSetsTask t) {
                                t.group = 'Insight'
                                t.description = "Displays all sourceSets available in project '$project.name'."
                            }
                        })

                    project.tasks.register('sourceSetSettings', SourceSetSettingsTask,
                        new Action<SourceSetSettingsTask>() {
                            @Override
                            void execute(SourceSetSettingsTask t) {
                                t.group = 'Insight'
                                t.description = 'Display the settings of a SourceSet.'
                            }
                        })

                    project.tasks.addRule('Pattern: <SourceSetName>SourceSetSettings: Displays the settings of a SourceSet.', new Action<String>() {
                        @Override
                        void execute(String sourceSetName) {
                            if (sourceSetName.endsWith('SourceSetSettings')) {
                                String resolvedSourceSetName = sourceSetName - 'SourceSetSettings'
                                project.tasks.register(sourceSetName, SourceSetSettingsTask,
                                    new Action<SourceSetSettingsTask>() {
                                        @Override
                                        void execute(SourceSetSettingsTask t) {
                                            t.group = 'Insight'
                                            t.sourceSet = resolvedSourceSetName
                                            t.description = "Display the settings of the '${resolvedSourceSetName}' sourceSet."
                                        }
                                    })
                            }
                        }
                    })
                }

                project.tasks.register('javaCompilerSettings', JavaCompilerSettingsTask,
                    new Action<JavaCompilerSettingsTask>() {
                        @Override
                        void execute(JavaCompilerSettingsTask t) {
                            t.group = 'Insight'
                            t.description = 'Display Java compiler settings.'
                        }
                    })

                project.tasks.addRule('Pattern: compile<SourceSetName>JavaSettings: Displays compiler settings of a JavaCompile task.', new Action<String>() {
                    @Override
                    void execute(String taskName) {
                        if (taskName.startsWith('compile') && taskName.endsWith('JavaSettings')) {
                            String resolvedTaskName = taskName - 'Settings'
                            project.tasks.register(taskName, JavaCompilerSettingsTask,
                                new Action<JavaCompilerSettingsTask>() {
                                    @Override
                                    void execute(JavaCompilerSettingsTask t) {
                                        t.group = 'Insight'
                                        t.task = resolvedTaskName
                                        t.description = "Display Java compiler settings of the '${resolvedTaskName}' task."
                                    }
                                })
                        }
                    }
                })

                project.tasks.register('testSettings', TestSettingsTask,
                    new Action<TestSettingsTask>() {
                        @Override
                        void execute(TestSettingsTask t) {
                            t.group = 'Insight'
                            t.description = 'Display test task settings.'
                        }
                    })

                project.tasks.addRule('Pattern: <SourceSetName>TestSettings: Displays settings of a Test task.', new Action<String>() {
                    @Override
                    void execute(String taskName) {
                        if (taskName.endsWith('TestSettings')) {
                            String resolvedTaskName = taskName - 'Settings'
                            project.tasks.register(taskName, TestSettingsTask,
                                new Action<TestSettingsTask>() {
                                    @Override
                                    void execute(TestSettingsTask t) {
                                        t.group = 'Insight'
                                        t.task = resolvedTaskName
                                        t.description = "Display settings of the '${resolvedTaskName}' task."
                                    }
                                })
                        }
                    }
                })

                project.tasks.register('jarSettings', JarSettingsTask,
                        new Action<JarSettingsTask>() {
                            @Override
                            void execute(JarSettingsTask t) {
                                t.group = 'Insight'
                                t.description = 'Display JAR settings.'
                            }
                        })

                project.tasks.addRule('Pattern: <JarName>JarSettings: Displays settings of a JAR task.', new Action<String>() {
                    @Override
                    void execute(String taskName) {
                        if (taskName.endsWith('JarSettings')) {
                            String resolvedTaskName = taskName - 'JarSettings'
                            resolvedTaskName = resolvedTaskName ?: 'jar'
                            project.tasks.register(taskName, JarSettingsTask,
                                    new Action<JarSettingsTask>() {
                                        @Override
                                        void execute(JarSettingsTask t) {
                                            t.group = 'Insight'
                                            t.task = resolvedTaskName
                                            t.description = "Display settings of the '${resolvedTaskName}' JAR task."
                                        }
                                    })
                        }
                    }
                })
            }
        })

        project.pluginManager.withPlugin('war', new Action<AppliedPlugin>() {
            @Override
            void execute(AppliedPlugin appliedPlugin) {
                project.tasks.register('warSettings', WarSettingsTask,
                        new Action<WarSettingsTask>() {
                            @Override
                            void execute(WarSettingsTask t) {
                                t.group = 'Insight'
                                t.description = 'Display WAR settings.'
                            }
                        })

                project.tasks.addRule('Pattern: <WarName>WarSettings: Displays settings of a WAR task.', new Action<String>() {
                    @Override
                    void execute(String taskName) {
                        if (taskName.endsWith('WarSettings')) {
                            String resolvedTaskName = taskName - 'WarSettings'
                            resolvedTaskName = resolvedTaskName ?: 'war'
                            project.tasks.register(taskName, WarSettingsTask,
                                    new Action<WarSettingsTask>() {
                                        @Override
                                        void execute(WarSettingsTask t) {
                                            t.group = 'Insight'
                                            t.task = resolvedTaskName
                                            t.description = "Display settings of the '${resolvedTaskName}' WAR task."
                                        }
                                    })
                        }
                    }
                })
            }
        })

        if (isRootProject(project)) {
            project.tasks.register('listProjects', ListProjectsTask,
                new Action<ListProjectsTask>() {
                    @Override
                    void execute(ListProjectsTask t) {
                        t.group = 'Insight'
                        t.description = 'List all projects.'
                    }
                })

            project.tasks.register('listIncludedBuilds', ListIncludedBuildsTask,
                new Action<ListIncludedBuildsTask>() {
                    @Override
                    void execute(ListIncludedBuildsTask t) {
                        t.group = 'Insight'
                        t.description = 'List all included builds.'
                    }
                })

            /*
            project.gradle.addBuildListener(new BuildAdapter() {
                @Override
                void projectsEvaluated(Gradle gradle) {
                    project.subprojects.each { Project subproject ->
                        PluginUtils.resolveEffectiveConfig(subproject).rootReady()
                    }
                    PluginUtils.resolveEffectiveConfig(project).rootReady()
                }
            })
            */
        }

        project.pluginManager.withPlugin('groovy-base', new Action<AppliedPlugin>() {
            @Override
            void execute(AppliedPlugin appliedPlugin) {
                project.tasks.register('groovyCompilerSettings', GroovyCompilerSettingsTask,
                        new Action<GroovyCompilerSettingsTask>() {
                            @Override
                            void execute(GroovyCompilerSettingsTask t) {
                                t.group = 'Insight'
                                t.description = 'Display Groovy compiler settings.'
                            }
                        })

                project.tasks.addRule('Pattern: compile<SourceSetName>GroovySettings: Displays compiler settings of a GroovyCompile task.', new Action<String>() {
                    @Override
                    void execute(String taskName) {
                        if (taskName.startsWith('compile') && taskName.endsWith('GroovySettings')) {
                            String resolvedTaskName = taskName - 'Settings'
                            project.tasks.register(taskName, GroovyCompilerSettingsTask,
                                    new Action<GroovyCompilerSettingsTask>() {
                                        @Override
                                        void execute(GroovyCompilerSettingsTask t) {
                                            t.group = 'Insight'
                                            t.task = resolvedTaskName
                                            t.description = "Display Groovy compiler settings of the '${resolvedTaskName}' task."
                                        }
                                    })
                        }
                    }
                })
            }
        })

        project.pluginManager.withPlugin('scala-base', new Action<AppliedPlugin>() {
            @Override
            void execute(AppliedPlugin appliedPlugin) {
                project.tasks.register('scalaCompilerSettings', ScalaCompilerSettingsTask,
                        new Action<ScalaCompilerSettingsTask>() {
                            @Override
                            void execute(ScalaCompilerSettingsTask t) {
                                t.group = 'Insight'
                                t.description = 'Display Scala compiler settings.'
                            }
                        })

                project.tasks.addRule('Pattern: compile<SourceSetName>ScalaSettings: Displays compiler settings of a ScalaCompile task.', new Action<String>() {
                    @Override
                    void execute(String taskName) {
                        if (taskName.startsWith('compile') && taskName.endsWith('ScalaSettings')) {
                            String resolvedTaskName = taskName - 'Settings'
                            project.tasks.register(taskName, ScalaCompilerSettingsTask,
                                    new Action<ScalaCompilerSettingsTask>() {
                                        @Override
                                        void execute(ScalaCompilerSettingsTask t) {
                                            t.group = 'Insight'
                                            t.task = resolvedTaskName
                                            t.description = "Display Scala compiler settings of the '${resolvedTaskName}' task."
                                        }
                                    })
                        }
                    }
                })
            }
        })

        project.pluginManager.withPlugin('org.jetbrains.kotlin.jvm', new Action<AppliedPlugin>() {
            @Override
            void execute(AppliedPlugin appliedPlugin) {
                project.tasks.register('kotlinCompilerSettings', KotlinCompilerSettingsTask,
                        new Action<KotlinCompilerSettingsTask>() {
                            @Override
                            void execute(KotlinCompilerSettingsTask t) {
                                t.group = 'Insight'
                                t.description = 'Display Kotlin compiler settings.'
                            }
                        })

                project.tasks.addRule('Pattern: compile<SourceSetName>KotlinSettings: Displays compiler settings of a KotlinCompile task.', new Action<String>() {
                    @Override
                    void execute(String taskName) {
                        if (taskName.startsWith('compile') && taskName.endsWith('KotlinSettings')) {
                            String resolvedTaskName = taskName - 'Settings'
                            project.tasks.register(taskName, KotlinCompilerSettingsTask,
                                    new Action<KotlinCompilerSettingsTask>() {
                                        @Override
                                        void execute(KotlinCompilerSettingsTask t) {
                                            t.group = 'Insight'
                                            t.task = resolvedTaskName
                                            t.description = "Display Kotlin compiler settings of the '${resolvedTaskName}' task."
                                        }
                                    })
                        }
                    }
                })
            }
        })

        project.afterEvaluate {
            project.tasks.withType(JavaExec, new Action<JavaExec>() {
                @Override
                void execute(JavaExec t) {
                    String resolvedTaskName = t.name
                    project.tasks.register(t.name + 'Settings', JavaExecSettingsTask,
                        new Action<JavaExecSettingsTask>() {
                            @Override
                            void execute(JavaExecSettingsTask s) {
                                s.group = 'Insight'
                                s.task = resolvedTaskName
                                s.description = "Display settings of the '${resolvedTaskName}' task."
                            }
                        })
                }
            })

            ProjectConfigurationExtension rootExtension = project.rootProject.extensions.findByType(ProjectConfigurationExtension)
            ProjectConfigurationExtension extension = project.extensions.findByType(ProjectConfigurationExtension)
            extension.normalize()

            boolean validate = PluginUtils.checkFlag(ORG_KORDAMP_GRADLE_BASE_VALIDATE, true)

            List<String> errors = []
            if (isRootProject(project)) {
                // extension == rootExtension
                ProjectConfigurationExtension merged = extension.postMerge()
                if (validate) errors.addAll(merged.validate())
                project.extensions.create(ProjectConfigurationExtension.EFFECTIVE_CONFIG_NAME, ProjectConfigurationExtension, merged)
            } else {
                // parent project may not have applied kordamp.base
                if (rootExtension) {
                    ProjectConfigurationExtension merged = extension.merge(rootExtension)
                    if (validate) errors.addAll(merged.validate())
                    project.extensions.create(ProjectConfigurationExtension.EFFECTIVE_CONFIG_NAME, ProjectConfigurationExtension, merged)
                } else {
                    extension = extension.postMerge()
                    if (validate) errors.addAll(extension.validate())
                    project.extensions.create(ProjectConfigurationExtension.EFFECTIVE_CONFIG_NAME, ProjectConfigurationExtension, extension)
                }
            }

            if (validate && errors) {
                errors.each { project.logger.error(it) }
                throw new GradleException("Project ${project.name} has not been properly configured")
            }
        }
    }

    static void applyIfMissing(Project project) {
        if (!project.plugins.findPlugin(BasePlugin)) {
            project.pluginManager.apply(BasePlugin)
        }
    }

    static boolean isRootProject(Project project) {
        project == project.rootProject
    }
}
