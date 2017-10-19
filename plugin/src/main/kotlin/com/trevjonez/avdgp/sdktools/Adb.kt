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

import com.trevjonez.avdgp.rx.never
import com.trevjonez.avdgp.rx.readLines
import com.trevjonez.avdgp.rx.toCompletable
import com.trevjonez.avdgp.rx.toObservable
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.slf4j.Logger
import java.io.File

class Adb(private val adbPath: File, private val logger: Logger) {

    private val whitespace = Regex("""\s""")

    fun runningEmulators(): Single<Set<Device>> {
        return ProcessBuilder(adbPath.absolutePath, "devices")
                .toObservable("adb", logger, Observable.never())
                .subscribeOn(Schedulers.io())
                .flatMap { (stdOut, stdErr) ->
                    Observable.merge(
                            stdErr.readLines()
                                    .subscribeOn(Schedulers.io())
                                    .doOnNext { logger.info("stdErr: $it") }
                                    .never(),
                            stdOut.readLines()
                                    .subscribeOn(Schedulers.io())
                                    .doOnNext { logger.info("stdOut: $it") }
                    )
                }
                .filter { it.endsWith("offline") || it.endsWith("device") || it.endsWith("unauthorized") }
                .map { it.split(whitespace).let { Device(it[0], Device.Status.fromString(it[1])) } }
                .filter { it.isEmulator }
                .collectInto(mutableSetOf<Device>()) { set, device -> set.add(device) }
                .map { it.toSet() }
    }

    fun kill(device: Device): Completable {
        if (!device.isEmulator)
            return Completable.error(IllegalArgumentException("Attempting to kill something other than an AVD"))

        //TODO perhaps do this with a socket so we know when the avd goes down?
        return ProcessBuilder(adbPath.absolutePath, "-s", device.id, "emu", "kill")
                .toCompletable("adb", logger)
    }

    data class Device(val id: String, val status: Status) {
        enum class Status(val adbOutput: String) {
            ONLINE("device"), UNAUTHORIZED("unauthorized"), OFFLINE("offline");

            companion object {
                fun fromString(stringRepresentation: String): Status {
                    Status.values().forEach {
                        if (stringRepresentation == it.adbOutput) return it
                    }
                    throw IllegalArgumentException("Unknown status: $stringRepresentation. Must be one of ${Status.values()}")
                }
            }
        }

        val isEmulator = id.startsWith("emulator-")
        fun emulatorPort(): Int {
            require(isEmulator) { "only emulators have ports assigned to them" }
            return id.removePrefix("emulator-").toInt()
        }
    }
}