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

sealed class NetDelay(open val cliValue: String) {

    object GSM : NetDelay(NetConstants.GSM)
    object HSCSD : NetDelay(NetConstants.HSCSD)
    object GPRS : NetDelay(NetConstants.GPRS)
    object EDGE : NetDelay(NetConstants.EDGE)
    object UMTS : NetDelay(NetConstants.UMTS)
    object HSDPA : NetDelay(NetConstants.HSDPA)
    object LTE : NetDelay(NetConstants.LTE)
    object EVDO : NetDelay(NetConstants.EVDO)
    object NONE : NetDelay(NetConstants.NONE)

    data class Other(override val cliValue: String) : NetDelay(cliValue)

    companion object {
        @JvmStatic
        fun from(value: String): NetDelay {
            return when (value.toUpperCase()) {
                NetConstants.GSM -> GSM
                NetConstants.HSCSD -> HSCSD
                NetConstants.GPRS -> GPRS
                NetConstants.EDGE -> EDGE
                NetConstants.UMTS -> UMTS
                NetConstants.HSDPA -> HSDPA
                NetConstants.LTE -> LTE
                NetConstants.EVDO -> EVDO
                NetConstants.NONE -> NONE
                else -> {
                    if (Regex("[0-9]+").matches(value) || Regex("[0-9]+:[0-9]+").matches(value)) Other(value)
                    else throw IllegalArgumentException("NetDelay must be of format '[0-9]+' or '[0-9]+:[0-9]+' or be one of: GSM, HSCSD, GPRS, EDGE, UMTS, HSDPA, LTE, EVDO, NONE")
                }
            }
        }
    }
}
