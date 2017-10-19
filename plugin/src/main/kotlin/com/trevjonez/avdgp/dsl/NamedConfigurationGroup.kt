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

package com.trevjonez.avdgp.dsl

import groovy.lang.Closure

open class NamedConfigurationGroup(val name: String) {

    val escapedName = name.replace(' ', '_')

    val avdConfig = AvdConfig()
    fun avd(configure: Closure<AvdConfig>) {
        configure.delegate = avdConfig
        configure.call()
    }

    val launchOptions: MutableList<String> = mutableListOf()

    fun launchOption(vararg values: String) {
        values.forEach { launchOptions.add(it) }
    }

    fun systemImageKey(): String {
        return "system-images;${avdConfig.api.cliValue};${avdConfig.type.cliValue};${avdConfig.abi.cpuArch}"
    }

    fun installTaskName(): String {
        return "installSystemImage_api${avdConfig.api.name}_${avdConfig.type.displayName()}_${avdConfig.abi.cpuArch}"
    }

    fun createTaskName(): String {
        return "createAvd_$escapedName"
    }

    fun startTaskName(): String {
        return "startAvd_$escapedName"
    }

    fun stopTaskName(): String {
        return "stopAvd_$escapedName"
    }
}