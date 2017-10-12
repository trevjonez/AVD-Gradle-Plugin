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

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class PluginTest {
    @get:Rule val testProjectDir = BuildDirFolder("PluginTest")

    @Test
    fun testForBasicTaskCreation() {

        testProjectDir.apply {
            File(file, "local.properties").writeText("sdk.dir=${System.getenv("ANDROID_HOME")}", Charsets.UTF_8)
        }

        GradleRunner.create()
                .withGradleVersion("4.1")
                .withProjectDir(testProjectDir.root)
                .withArguments("tasks", "--stacktrace")
                .withDebug(true)
                .forwardOutput()
                .build()
    }
}