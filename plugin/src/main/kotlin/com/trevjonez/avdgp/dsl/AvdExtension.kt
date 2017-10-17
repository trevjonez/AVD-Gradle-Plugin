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
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import java.io.File

open class AvdExtension(project: Project) {
    companion object {
        val avdNameRegex = Regex("""[a-zA-Z0-9._() -]+""")
    }

    val configs: NamedDomainObjectContainer<NamedConfigurationGroup> =
            project.container(NamedConfigurationGroup::class.java) { name ->
                require(name matches avdNameRegex) { "AVD name must be of form `${avdNameRegex.pattern}`: $name" }
                NamedConfigurationGroup(name)
            }

    fun configs(closure: Closure<Any>) {
        configs.configure(closure)
    }

    var acceptAndroidSdkPreviewLicense = false
    fun acceptAndroidSdkPreviewLicense(value: Boolean) {
        acceptAndroidSdkPreviewLicense = value
    }

    var acceptAndroidSdkLicense = false
    fun acceptAndroidSdkLicense(value: Boolean) {
        acceptAndroidSdkLicense = value
    }

    var autoUpdate: Boolean = true
    fun autoUpdate(value: Boolean) {
        autoUpdate = value
    }

    var proxyType: String? = null
    fun proxyType(value: String?) {
        proxyType = value
    }

    var proxyHost: String? = null
    fun proxyHost(value: String?) {
        proxyHost = value
    }

    var proxyPort: Int? = null
    fun proxyPort(value: Int?) {
        proxyPort = value
    }

    var noHttps: Boolean = false
    fun noHttps(value: Boolean) {
        noHttps = value
    }

    var avdPath: File? = null
    fun avdPath(value: File?) {
        avdPath = value
    }
}