/*
 * Copyright (c) 2017. Trevor Jones
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trevjonez.avdgp

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class PluginTest {
    @get:Rule
    val testDir = BuildDirFolder("PluginTest")

    @Test
    fun testForBasicTaskCreation() {
        var projectDir: File? = null
        var sdkDir: File? = null
        testDir.file.apply {
            childDirectory("Android") {
                sdkDir = childDirectory("sdk") {
                    File(System.getProperty("sdkToolsPath"))
                            .copyRecursively(childFile("tools"))
                }
            }

            projectDir = childDirectory("sampleProject") {
                childFile("local.properties").writeText("sdk.dir=${sdkDir?.absolutePath}")
                @Language("Groovy")
                val buildFile = """
                    buildscript {
                        repositories {
                            google()
                            jcenter()
                            mavenLocal()
                        }
                        dependencies {
                            classpath "com.github.trevjonez:AVD-Gradle-Plugin:${System.getProperty("avd_plugin_version")}"
                        }
                    }
                    apply plugin: 'AVD'

                    AVD.configs {
                        "Nexus 5x API O" {
                            avd {
                                abi "x86_64"
                                api 26
                                type "google_apis"
                                deviceId "Nexus 5X"
                            }
                        }
                    }
                """.trimIndent()
                childFile("build.gradle").writeText(buildFile)
            }
        }

        val buildResult = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("tasks", "--stacktrace")
                .withDebug(true)
                .forwardOutput()
                .build()

        assertThat(buildResult.output).contains("installSystemImage_api26_GoogleApis_x86_64")
    }

    @Test
    fun illegalAvdNameShouldFailConfigurationPhase() {

        var projectDir: File? = null
        var sdkDir: File? = null
        testDir.file.apply {
            childDirectory("Android") {
                sdkDir = childDirectory("sdk") {
                    File(System.getProperty("sdkToolsPath"))
                            .copyRecursively(childFile("tools"))
                }
            }

            projectDir = childDirectory("sampleProject") {
                childFile("local.properties").writeText("sdk.dir=${sdkDir?.absolutePath}")
                @Language("Groovy")
                val buildFile = """
                    buildscript {
                        repositories {
                            google()
                            jcenter()
                            mavenLocal()
                        }
                        dependencies {
                            classpath "com.github.trevjonez:AVD-Gradle-Plugin:${System.getProperty("avd_plugin_version")}"
                        }
                    }
                    apply plugin: 'AVD'

                    AVD.configs {
                        "Nexus 5x API O**" {
                            avd {
                                abi "x86_64"
                                api 26
                                type "google_apis"
                                deviceId "Nexus 5X"
                            }
                        }
                    }
                """.trimIndent()
                childFile("build.gradle").writeText(buildFile)
            }
        }

        val buildResult = GradleRunner.create()
                .withProjectDir(projectDir)
                .withDebug(true)
                .forwardOutput()
                .buildAndFail()

        assertThat(buildResult.output).contains("AVD name must be of form ")
    }

    @Test
    fun packageNotFoundThrowsException() {

        var projectDir: File? = null
        var sdkDir: File? = null
        testDir.file.apply {
            childDirectory("Android") {
                sdkDir = childDirectory("sdk") {
                    File(System.getProperty("sdkToolsPath"))
                            .copyRecursively(childFile("tools"))
                    childDirectory("tools") {
                        childDirectory("bin") {
                            ProcessBuilder("chmod", "+x", childFile("sdkmanager").absolutePath).start().waitFor(2, TimeUnit.SECONDS)
                            ProcessBuilder("chmod", "+x", childFile("avdmanager").absolutePath).start().waitFor(2, TimeUnit.SECONDS)
                        }
                    }
                }
            }

            projectDir = childDirectory("sampleProject") {
                childFile("local.properties").writeText("sdk.dir=${sdkDir?.absolutePath}")
                @Language("Groovy")
                val buildFile = """
                    buildscript {
                        repositories {
                            google()
                            jcenter()
                            mavenLocal()
                        }
                        dependencies {
                            classpath "com.github.trevjonez:AVD-Gradle-Plugin:${System.getProperty("avd_plugin_version")}"
                        }
                    }
                    apply plugin: 'AVD'

                    AVD.configs {
                        "Nexus 5x API O" {
                            avd {
                                abi "x86_64"
                                api 26
                                type "google_apis"
                                deviceId "Nexus 5X"
                            }
                        }
                    }
                """.trimIndent()
                childFile("build.gradle").writeText(buildFile)
            }
        }

        val buildResult = GradleRunner.create()
                .withProjectDir(projectDir)
                .withDebug(true)
                .withArguments("installSystemImage_api26_GoogleApis_x86_64", "--stacktrace", "--info")
                .forwardOutput()
                .buildAndFail()

        assertThat(buildResult.output).contains("""
            * What went wrong:
            Execution failed for task ':installSystemImage_api26_GoogleApis_x86_64'.
            > Failed to find package system-images;android-26;google_apis;x86_64
        """.trimIndent())
    }

    @Test
    fun failToGrantPermissionFailsBuild() {
        var projectDir: File? = null
        var sdkDir: File? = null
        testDir.file.apply {
            childDirectory("Android") {
                sdkDir = childDirectory("sdk") {
                    File(System.getProperty("sdkToolsPath"))
                            .copyRecursively(childFile("tools"))
                    childDirectory("tools") {
                        childDirectory("bin") {
                            ProcessBuilder("chmod", "+x", childFile("sdkmanager").absolutePath).start().waitFor(2, TimeUnit.SECONDS)
                            ProcessBuilder("chmod", "+x", childFile("avdmanager").absolutePath).start().waitFor(2, TimeUnit.SECONDS)
                        }
                    }
                }
            }

            projectDir = childDirectory("sampleProject") {
                childFile("local.properties").writeText("sdk.dir=${sdkDir?.absolutePath}")
                @Language("Groovy")
                val buildFile = """
                    buildscript {
                        repositories {
                            google()
                            jcenter()
                            mavenLocal()
                        }
                        dependencies {
                            classpath "com.github.trevjonez:AVD-Gradle-Plugin:${System.getProperty("avd_plugin_version")}"
                        }
                    }
                    apply plugin: 'AVD'

                    AVD.configs {
                        "Nexus 5x API O" {
                            avd {
                                abi "x86"
                                api 26
                                type "google_apis"
                                deviceId "Nexus 5X"
                            }
                        }
                    }
                """.trimIndent()
                childFile("build.gradle").writeText(buildFile)
            }
        }

        val buildResult = GradleRunner.create()
                .withProjectDir(projectDir)
                .withDebug(true)
                .withArguments("installSystemImage_api26_GoogleApis_x86", "--stacktrace", "--info")
                .forwardOutput()
                .buildAndFail()

        assertThat(buildResult.output).contains("""
            * What went wrong:
            Execution failed for task ':installSystemImage_api26_GoogleApis_x86'.
            > Can not automatically accept license type: Sdk. You must manually install or grant AVD plugin permission to auto agree
        """.trimIndent())
    }

    @Test
    fun installTaskSucceeds() {
        var projectDir: File? = null
        var sdkDir: File? = null
        testDir.file.apply {
            childDirectory("Android") {
                sdkDir = childDirectory("sdk") {
                    File(System.getProperty("sdkToolsPath"))
                            .copyRecursively(childFile("tools"))
                    childDirectory("tools") {
                        childDirectory("bin") {
                            ProcessBuilder("chmod", "+x", childFile("sdkmanager").absolutePath).start().waitFor(2, TimeUnit.SECONDS)
                            ProcessBuilder("chmod", "+x", childFile("avdmanager").absolutePath).start().waitFor(2, TimeUnit.SECONDS)
                        }
                    }
                }
            }

            projectDir = childDirectory("sampleProject") {
                childFile("local.properties").writeText("sdk.dir=${sdkDir?.absolutePath}")
                @Language("Groovy")
                val buildFile = """
                    buildscript {
                        repositories {
                            google()
                            jcenter()
                            mavenLocal()
                        }
                        dependencies {
                            classpath "com.github.trevjonez:AVD-Gradle-Plugin:${System.getProperty("avd_plugin_version")}"
                        }
                    }
                    apply plugin: 'AVD'

                    AVD {
                        configs {
                            "Nexus 5x API O" {
                                avd {
                                    abi "x86"
                                    api 26
                                    type "google_apis"
                                    deviceId "Nexus 5X"
                                }
                            }
                        }
                        acceptAndroidSdkLicense true
                    }
                """.trimIndent()
                childFile("build.gradle").writeText(buildFile)
            }
        }

        val buildResult = GradleRunner.create()
                .withProjectDir(projectDir)
                .withDebug(true)
                .withArguments("installSystemImage_api26_GoogleApis_x86", "--stacktrace", "--info")
                .forwardOutput()
                .build()
    }

    private fun File.childDirectory(dirName: String, block: File.() -> Unit = {}): File {
        return File(this, dirName).apply {
            mkdir()
            block()
        }
    }

    private fun File.childFile(name: String) = File(this, name)
}