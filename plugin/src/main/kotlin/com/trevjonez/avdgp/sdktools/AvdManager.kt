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

package com.trevjonez.avdgp.sdktools

import com.android.prefs.AndroidLocation
import com.trevjonez.avdgp.rx.*
import io.reactivex.Completable
import io.reactivex.Observable
import org.slf4j.Logger
import java.io.File
import java.util.*

class AvdManager(private val avdManager: File,
                 private val logger: Logger,
                 private val avdPath: File? = null) {

    fun createAvd(name: String, sdkKey: String, coreCount: Int, options: List<String>, iniPatches: List<Pair<String, String>>): Completable {
        val args = mutableListOf<String>(
                avdManager.absolutePath,
                "create", "avd",
                "--name", name,
                "--package", sdkKey)
        args.addAll(options)
        return ProcessBuilder(args)
                .also { builder ->
                    avdPath?.let { builder.environment().put("ANDROID_AVD_HOME", it.absolutePath) }
                }
                .toCompletable("avdmanager", logger)
                .doOnError { logger.info("avdmanager create avd threw") }
                .andThen { observer ->
                    try {
                        val avdDir = File(avdPath?.absolutePath ?: "${AndroidLocation.getFolder()}${File.separator}avd", "$name.avd")
                        val configIni = File(avdDir, "config.ini")
                        val properties = Properties()
                        properties.load(configIni.inputStream())
                        val height = properties["hw.lcd.height"]
                        val width = properties["hw.lcd.width"]
                        properties.setProperty("skin.name", "${width}x$height")
                        properties.setProperty("hw.cput.ncore", coreCount.toString())
                        properties.setProperty("hw.keyboard", "yes")
                        iniPatches.forEach { (k, v) -> properties.setProperty(k, v) }
                        configIni.writer().use {
                            properties.store(it, "Created by AVD-Gradle-Plugin via avdmanager")
                        }
                        observer.onComplete()
                    } catch (error: Throwable) {
                        logger.info("error while modifying ini file")
                        observer.onError(error)
                    }
                }
    }

    fun listAvd(): List<String> {
        return ProcessBuilder(avdManager.absolutePath, "list", "avd", "-c")
                .also { builder ->
                    avdPath?.let { builder.environment().put("ANDROID_AVD_HOME", it.absolutePath) }
                }
                .toObservable("avdmanager", logger, Observable.never())
                .doOnError { logger.info("avdmanager list avd threw")}
                .flatMap { (stdOut, stdErr) ->
                    Observable.merge(
                            stdErr.readLines()
                                    .doOnNext { logger.info("stdErr: $it") }
                                    .doOnError { logger.info("stdErr threw: avdmanager list avd") }
                                    .never()
                                    .map<Collector> { TODO() },

                            stdOut.drain()
                                    .doOnError { logger.info("stdOut threw: avdmanager list avd") }
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
                .map { it.value.trim() }
                .toList()
                .blockingGet()
                .also {
                    logger.info("avdList ${it.joinToString()}")
                }
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