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
import com.trevjonez.avdgp.dsl.ProxyConfig
import com.trevjonez.avdgp.sdktools.SdkManager
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class InstallSystemImageTask : DefaultTask() {

    lateinit var sdkPath: File
    lateinit var configGroup: NamedConfigurationGroup
    var acceptSdkLicense = false
    var acceptSdkPreviewLicense = false
    var acceptHaxmLicense = false
    var autoUpdate = true
    var proxyConfig: ProxyConfig? = null
    var noHttps = false

    private val imageDir: File
        get() {
            return File(sdkPath, "system-images" +
                    File.separator + configGroup.avdConfig.api.cliValue +
                    File.separator + configGroup.avdConfig.type.cliValue +
                    File.separator + configGroup.avdConfig.abi)
        }

    private val manager by lazy { SdkManager(File(sdkPath, "tools${File.separator}bin${File.separator}sdkmanager"), logger, proxyConfig, noHttps) }

    init {
        outputs.upToDateWhen {
            val (obs, _) = manager.install(configGroup.systemImageKey())

            var error: Throwable? = null
            var hasPendingUpdate = false

            obs.doOnNext {
                if (it is SdkManager.InstallStatus.AwaitingLicense) {
                    hasPendingUpdate = true
                }
            }
                    .takeUntil { it is SdkManager.InstallStatus.AwaitingLicense }
                    .blockingSubscribe({ }, { error = it },
                            { logger.info("Install Available Check Complete \"${configGroup.systemImageKey()}\"") })

            error?.let { throw it }

            when {
                !imageDir.exists() -> false
                hasPendingUpdate && autoUpdate -> false
                else -> true
            }
        }
    }

    @TaskAction
    fun invoke() {
        val (obs, consoleInput) = manager.install(configGroup.systemImageKey())

        var error: Throwable? = null

        obs.blockingSubscribe({ status ->
            if (status is SdkManager.InstallStatus.AwaitingLicense) {
                val isApproved = when (status.licenseType) {
                    SdkManager.LicenseType.Sdk -> acceptSdkLicense
                    SdkManager.LicenseType.SdkPreview -> acceptSdkPreviewLicense
                    SdkManager.LicenseType.Haxm -> acceptHaxmLicense
                }
                if (isApproved) {
                    consoleInput("Y")
                } else {
                    throw IllegalStateException(
                            "Can not automatically accept license type: ${status.licenseType}. " +
                                    "You must manually install or grant AVD plugin permission to auto agree")
                }
            }
        }, { error = it }, { logger.info("Install Complete \"${configGroup.systemImageKey()}\"") })

        error?.let { throw it }
    }
}

