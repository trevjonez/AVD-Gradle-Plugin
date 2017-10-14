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
import org.gradle.api.tasks.TaskAction
import java.io.File

open class InstallSystemImageTask : DefaultTask() {

    lateinit var sdkPath: File
    lateinit var abi: Abi
    lateinit var api: ApiLevel
    lateinit var type: ApiType
    var acceptSdkLicense = false
    var acceptSdkPreviewLicense = false
    var autoUpdate = true

    private val imageDir: File
        get() {
            return File(sdkPath, "system-images" +
                    File.separator + api.cliValue +
                    File.separator + type.cliValue +
                    File.separator + abi.cpuArch)
        }

    init {
        outputs.upToDateWhen {
            val manager = SdkManager(File(sdkPath, "tools${File.separator}bin${File.separator}sdkmanager"), logger)
            val (obs, _) = manager.install(systemImageKey())

            var error: Throwable? = null
            var hasPendingUpdate = false

            obs.doOnNext {
                if (it is SdkManager.InstallStatus.AwaitingLicense) {
                    hasPendingUpdate = true
                }
            }
                    .takeUntil { it is SdkManager.InstallStatus.AwaitingLicense }
                    .blockingSubscribe({ }, { error = it },
                            { logger.info("Install Available Check Complete \"${systemImageKey()}\"") })

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
        val manager = SdkManager(File(sdkPath, "tools${File.separator}bin${File.separator}sdkmanager"), logger)
        val (obs, callback) = manager.install(systemImageKey())

        var error: Throwable? = null

        obs.blockingSubscribe({ status ->
            if (status is SdkManager.InstallStatus.AwaitingLicense) {
                val isApproved = when (status.licenseType) {
                    SdkManager.LicenseType.Sdk -> acceptSdkLicense
                    SdkManager.LicenseType.SdkPreview -> acceptSdkPreviewLicense
                }
                if (isApproved) {
                    callback("Y")
                } else {
                    throw IllegalStateException(
                            "Can not automatically accept license type: ${status.licenseType}. " +
                                    "You must manually install or grant AVD plugin permission to auto agree")
                }
            }
        }, { error = it }, { logger.info("Install Complete \"${systemImageKey()}\"") })

        error?.let { throw it }
    }

    private fun systemImageKey(): String {
        return "system-images;${api.cliValue};${type.cliValue};${abi.cpuArch}"
    }

}

