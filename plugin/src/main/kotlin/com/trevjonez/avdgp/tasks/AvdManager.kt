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

import io.reactivex.Completable
import io.reactivex.Observable
import org.slf4j.Logger
import java.io.File

class AvdManager(private val avdManager: File,
                 private val logger: Logger) {

    //avdmanager create avd --name '26_6P_Playstore' --package 'system-images;android-26;google_apis_playstore;x86' --device 'Nexus 6P' --tag 'google_apis_playstore'
    fun createAvd(name: String, sdkKey: String, options: List<String>): Completable {
        val args = mutableListOf<String>(
                avdManager.absolutePath,
                "create", "avd",
                "--name", name,
                "--package", sdkKey)
        args.addAll(options)
        return ProcessBuilder(args).toCompletable("avdmanager", logger)
    }

    fun listAvd(): List<String> {
        return ProcessBuilder(avdManager.absolutePath, "list", "avd", "-c")
                .toObservable("avdmanager", logger, Observable.never())
                .flatMap { (stdOut, stdErr) ->
                    Observable.merge(
                            stdErr.readLines()
                                    .doOnNext { logger.error("stdErr: $it") }
                                    .never()
                                    .map<Collector> { TODO() },

                            stdOut.drain()
                                    .scan<Collector>(Collector.Unknown("")) { last, next ->
                                        if (last.done) {
                                            Collector.Unknown(next.toString())
                                        } else {
                                            val value = last.value + next
                                            if (value.startsWith("Parsing")) {
                                                Collector.Parsing(value)
                                            } else if (next == '\n' || next == '\r') {
                                                Collector.AvdName(value)
                                            } else {
                                                Collector.Unknown(value)
                                            }
                                        }
                                    }
                    )
                }
                .filter { it.done }
                .doOnNext { logger.info("stdOut: ${it.value}") }
                .ofType(Collector.AvdName::class.java)
                .map { it.value }
                .toList()
                .blockingGet()
    }

    private sealed class Collector {
        abstract val value: String
        abstract val done: Boolean

        data class Unknown(override val value: String) : Collector() {
            override val done = false
        }

        data class AvdName(override val value: String) : Collector() {
            override val done = true
        }

        data class Parsing(override val value: String) : Collector() {
            override val done = value.endsWith(".xml")
        }
    }
}