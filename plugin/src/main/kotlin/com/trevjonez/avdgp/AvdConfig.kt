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

import com.android.sdklib.SdkVersionInfo
import com.android.sdklib.devices.Abi

open class AvdConfig(val name: String) {
    var abi = Abi.X86_64
    var api = SdkVersionInfo.HIGHEST_KNOWN_STABLE_API
    var sdPath: String? = null
    var sdSize: String? = null
    var skin = "nexus_5x"
    var type = "google_apis"
    var port: Int? = null
    var launch_options: String? = null
    var wipe_data = true
    var use_data: String? = null
    var autoUpdate = false
}