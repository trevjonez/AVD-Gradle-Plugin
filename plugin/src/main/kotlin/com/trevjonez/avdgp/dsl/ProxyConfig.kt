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

data class ProxyConfig(val type: String, val host: String, val port: Int) {
    companion object {
        fun checkParams(type: String?, host: String?, port: Int?) {
            require(type != null) { "Missing proxy type for valid proxy config"}
            require(host != null) { "Missing host for valid proxy config"}
            require(port != null) { "Missing port for valid proxy config"}
        }
    }
}