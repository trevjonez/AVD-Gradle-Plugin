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

data class NetDelay internal constructor(val cliValue: String) {

    companion object {
        @JvmStatic val GSM = NetDelay("gsm")
        @JvmStatic val HSCSD = NetDelay("hscsd")
        @JvmStatic val GPRS = NetDelay("gprs")
        @JvmStatic val EDGE = NetDelay("edge")
        @JvmStatic val UMTS = NetDelay("umts")
        @JvmStatic val HSDPA = NetDelay("hsdpa")
        @JvmStatic val LTE = NetDelay("lte")
        @JvmStatic val EVDO = NetDelay("evdo")
        @JvmStatic val NONE = NetDelay("none")

        @JvmStatic
        fun from(value: String): NetDelay {
            return when (value.toUpperCase()) {
                "GSM" -> GSM
                "HSCSD" -> HSCSD
                "GPRS" -> GPRS
                "EDGE" -> EDGE
                "UMTS" -> UMTS
                "HSDPA" -> HSDPA
                "LTE" -> LTE
                "EVDO" -> EVDO
                "NONE" -> NONE
                else -> {
                    if (Regex("[0-9]+").matches(value) || Regex("[0-9]+:[0-9]+").matches(value)) NetDelay(value)
                    else throw IllegalArgumentException("NetDelay must be of format '[0-9]+' or '[0-9]+:[0-9]+' or be one of: GSM, HSCSD, GPRS, EDGE, UMTS, HSDPA, LTE, EVDO, NONE")
                }
            }
        }
    }
}
