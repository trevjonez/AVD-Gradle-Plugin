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
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CreateAvdTask : DefaultTask() {
    lateinit var sdkPath: File
    lateinit var configGroup: NamedConfigurationGroup

    private val avdManager by lazy {
        AvdManager(File(sdkPath, "tools${File.separator}bin${File.separator}avdmanager"), logger)
    }

    init {
        outputs.upToDateWhen {
            if (configGroup.avdConfig.forceCreate) false
            else {
                avdManager.listAvd().contains(configGroup.escapedName)
            }
        }
    }

    @TaskAction
    fun invoke() {
        val options = mutableListOf<String>()
        configGroup.avdConfig.deviceId?.let {
            options.add("--device")
            options.add(it)
        }

        configGroup.avdConfig.sdPath?.let {
            options.add("--sdcard")
            options.add(it.absolutePath)
        }

        configGroup.avdConfig.sdSize?.let {
            options.add("--sdcard")
            options.add(it)
        }

        configGroup.avdConfig.path?.let {
            options.add("--path")
            options.add(it.absolutePath)
        }

        if (configGroup.avdConfig.forceCreate) {
            options.add("--force")
        }

        if (configGroup.avdConfig.snapshot) {
            options.add("--snapshot")
        }

        avdManager.createAvd(configGroup.escapedName, configGroup.systemImageKey(), options)
                .blockingAwait()
    }
}