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

enum class ApiType(val cliValue: String) {
    DEFAULT("default"),
    GOOGLE_API("google_apis"),
    GOOGLE_PLAYSTORE("google_apis_playstore"),
    ANDROID_TV("android-tv"),
    ANDROID_WEAR("android-wear");

    companion object {
        @JvmStatic fun from(value: String): ApiType {
            return when(value.toLowerCase()) {
                "default" -> DEFAULT
                "google_apis" -> GOOGLE_API
                "google_apis_playstore" -> GOOGLE_PLAYSTORE
                "android-tv" -> ANDROID_TV
                "android-wear" -> ANDROID_WEAR
                else -> throw IllegalArgumentException("unable to match '$value' to an api type")
            }
        }
    }
}