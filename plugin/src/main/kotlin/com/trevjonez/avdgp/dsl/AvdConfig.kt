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

import com.android.sdklib.SdkVersionInfo
import com.android.sdklib.devices.Abi
import java.io.File

class AvdConfig {
    var abi = Abi.X86_64
    var api: ApiLevel = ApiLevel.from(SdkVersionInfo.HIGHEST_KNOWN_STABLE_API)
    var type = ApiType.GOOGLE_API
    var deviceId: String? = null
    var sdSize: String? = null
    var forceCreate = false
    var sdPath: File? = null
    var path: File? = null
    var snapshot = false
    var coreCount = Runtime.getRuntime().availableProcessors()
        set(value) {
            require(value >= 1)
            val available = Runtime.getRuntime().availableProcessors()
            field = if (value > available) available else value
        }
    val appendToConfigIni = mutableListOf<Pair<String,String>>().apply {
        add("skin.dynamic" to "yes")
        add("showDeviceFrame" to "no")
        add("skin.path" to "_no_skin")
        add("skin.path.backup" to "_no_skin")
        add("hw.gps" to "yes")
        add("hw.gpu.enabled" to "yes")
        add("hw.gpu.mode" to "auto")
    }

    fun abi(abi: Abi) {
        this.abi = abi
    }

    fun abi(abi: String) {
        this.abi = Abi.getEnum(abi) ?: throw IllegalArgumentException("No such abi '$abi'. Valid args: ${validAbiArgs()}")
    }

    fun api(api: ApiLevel) {
        this.api = api
    }

    fun api(api: Int) {
        this.api = ApiLevel.from(api)
    }

    fun api(api: String) {
        this.api = ApiLevel.from(api)
    }

    fun type(type: ApiType) {
        this.type = type
    }

    fun type(type: String) {
        this.type = ApiType.from(type)
    }

    fun deviceId(deviceId: String) {
        this.deviceId = deviceId
    }

    fun sdSize(sdSize: String) {
        this.sdSize = sdSize
    }

    fun forceCreate(forceCreate: Boolean) {
        this.forceCreate = forceCreate
    }

    fun coreCount(value: Int) {
        coreCount = value
    }

    fun configIniProperty(key: String, value: String) {
        appendToConfigIni.add(key to value)
    }
}