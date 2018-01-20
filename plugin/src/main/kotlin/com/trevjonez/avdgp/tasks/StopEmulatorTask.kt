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

package com.trevjonez.avdgp.tasks

import com.trevjonez.avdgp.dsl.NamedConfigurationGroup
import com.trevjonez.avdgp.sdktools.Adb
import com.trevjonez.avdgp.sdktools.AvdDeviceNameTransformer
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.TimeUnit

open class StopEmulatorTask : DefaultTask() {
    lateinit var sdkPath: File
    lateinit var configGroup: NamedConfigurationGroup

    private val adb by lazy {
        Adb(File(sdkPath, "platform-tools${File.separator}adb"), logger)
    }

    private val deviceNameTransformer by lazy {
        AvdDeviceNameTransformer(logger)
    }

    @TaskAction
    fun invoke() {
        val error = adb.runningEmulators()
                .compose(deviceNameTransformer)
                .map { it[configGroup.escapedName]!! }
                .flatMapCompletable { adb.kill(it) }
                .timeout(20, TimeUnit.SECONDS)
                .blockingGet()

        if (error != null && error !is NullPointerException) {
            throw error
        }
    }
}