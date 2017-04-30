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

import com.android.utils.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class PluginTest {
    @Rule @JvmField var testProjectDir = TemporaryFolder()

    @Test
    @Throws(Exception::class)
    fun testProjectBuild() {
        invokeBuild(copyProjectToTempFolder("test-project"))
    }

    private fun copyProjectToTempFolder(projectDir: String): File {
        return testProjectDir.newFolder(projectDir).also {
            FileUtils.copyDirectory(File(javaClass.classLoader.getResource(projectDir).path), it)
            File(it, "local.properties").writeText("sdk.dir=${System.getenv("HOME")}/Library/Android/sdk", Charsets.UTF_8)
            File(it, "libs").also {
                it.mkdir()
                FileUtils.copyFileToDirectory(File(".", "build/libs/plugin.jar"), it)
            }
        }
    }

    private fun invokeBuild(projectDir: File): BuildResult {
        return GradleRunner.create()
                .withGradleVersion("3.4.1")
                .withProjectDir(projectDir)
                .withArguments("tasks", "assembleDebug", "--stacktrace")
                .withDebug(true)
                .forwardOutput()
                .build()
    }
}