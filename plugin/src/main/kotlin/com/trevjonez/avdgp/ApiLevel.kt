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

data class ApiLevel(val cliValue: String) {

    companion object {
        @JvmStatic val `14` = ApiLevel("android-14")
        @JvmStatic val `15` = ApiLevel("android-15")
        @JvmStatic val `16` = ApiLevel("android-16")
        @JvmStatic val `17` = ApiLevel("android-17")
        @JvmStatic val `18` = ApiLevel("android-18")
        @JvmStatic val `19` = ApiLevel("android-19")
        @JvmStatic val `21` = ApiLevel("android-21")
        @JvmStatic val `22` = ApiLevel("android-22")
        @JvmStatic val `23` = ApiLevel("android-23")
        @JvmStatic val `24` = ApiLevel("android-24")
        @JvmStatic val `25` = ApiLevel("android-25")
        @JvmStatic val `26` = ApiLevel("android-O")

        @JvmStatic fun from(value: String): ApiLevel {
            return when (value.toUpperCase()) {
                "14" -> `14`
                "15", "I" -> `15`
                "16" -> `16`
                "17" -> `17`
                "18", "J" -> `18`
                "19", "k" -> `19`
                "21" -> `21`
                "22", "L" -> `22`
                "23", "M" -> `23`
                "24" -> `24`
                "25", "N" -> `25`
                "26", "O" -> `26`
                else -> throw IllegalArgumentException("Unable to match '$value' to an api level")
            }
        }

        @JvmStatic fun from(value: Int): ApiLevel {
            return when (value) {
                14 -> `14`
                15 -> `15`
                16 -> `16`
                17 -> `17`
                18 -> `18`
                19 -> `19`
                21 -> `21`
                22 -> `22`
                23 -> `23`
                24 -> `24`
                25 -> `25`
                26 -> `26`
                else -> throw IllegalArgumentException("Unable to match '$value' to an api level")
            }
        }
    }
}