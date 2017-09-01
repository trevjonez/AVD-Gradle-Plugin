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

sealed class NetSpeed(open val cliValue: String) {
    object GSM : NetSpeed(NetConstants.GSM)
    object HSCSD : NetSpeed(NetConstants.HSCSD)
    object GPRS : NetSpeed(NetConstants.GPRS)
    object EDGE : NetSpeed(NetConstants.EDGE)
    object UMTS : NetSpeed(NetConstants.UMTS)
    object HSDPA : NetSpeed(NetConstants.HSDPA)
    object LTE : NetSpeed(NetConstants.LTE)
    object EVDO : NetSpeed(NetConstants.EVDO)
    object FULL : NetSpeed(NetConstants.FULL)

    data class Other(override val cliValue: String) : NetSpeed(cliValue)

    companion object {
        @JvmStatic
        fun from(value: String): NetSpeed {
            return when (value.toLowerCase()) {
                NetConstants.GSM -> GSM
                NetConstants.HSCSD -> HSCSD
                NetConstants.GPRS -> GPRS
                NetConstants.EDGE -> EDGE
                NetConstants.UMTS -> UMTS
                NetConstants.HSDPA -> HSDPA
                NetConstants.LTE -> LTE
                NetConstants.EVDO -> EVDO
                NetConstants.FULL -> FULL
                else -> {
                    if (Regex("[0-9]+").matches(value) || Regex("[0-9]+:[0-9]+").matches(value)) NetSpeed.Other(value)
                    else throw IllegalArgumentException("NetSpeed must be of format '[0-9]+' or '[0-9]+:[0-9]+' or be one of: GSM, HSCSD, GPRS, EDGE, UMTS, HSDPA, LTE, EVDO, FULL")
                }
            }
        }
    }
}