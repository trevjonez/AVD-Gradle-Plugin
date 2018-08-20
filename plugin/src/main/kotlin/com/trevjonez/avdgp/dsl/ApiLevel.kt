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

enum class ApiLevel(val cliValue: String) {

    `14`("android-14"),
    `15`("android-15"),
    `16`("android-16"),
    `17`("android-17"),
    `18`("android-18"),
    `19`("android-19"),
    `21`("android-21"),
    `22`("android-22"),
    `23`("android-23"),
    `24`("android-24"),
    `25`("android-25"),
    `26`("android-26"),
    `27`("android-27"),
    `28`("android-28");

    companion object {

        @JvmStatic
        fun from(value: String): ApiLevel {
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
                "27" -> `27`
                "28", "P" -> `28`
                else -> throw IllegalArgumentException("Unable to match '$value' to an api level")
            }
        }

        @JvmStatic
        fun from(value: Int): ApiLevel {
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
                27 -> `27`
                28 -> `28`
                else -> throw IllegalArgumentException("Unable to match '$value' to an api level")
            }
        }
    }
}