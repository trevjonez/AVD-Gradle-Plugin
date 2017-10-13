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

import com.android.sdklib.devices.Abi
import com.trevjonez.avdgp.dsl.ApiLevel
import com.trevjonez.avdgp.dsl.ApiType
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import java.io.File

open class InstallSystemImageTask : DefaultTask() {

    @get:Input
    lateinit var sdkPath: File

    @get:Input
    lateinit var abi: Abi

    @get:Input
    lateinit var api: ApiLevel

    @get:Input
    lateinit var type: ApiType

    fun invoke() {
        val manager = SdkManager(File(sdkPath, "tools${File.separator}bin${File.separator}sdkmanager"))
        val (obs, callback) = manager.install(systemImageKey())
        obs.subscribe()
    }

    fun systemImageKey(): String {
        return "system-images;${api.cliValue};${type.cliValue};${abi.cpuArch}"
    }
}

