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
import com.trevjonez.avdgp.rx.toObservable
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.slf4j.Logger
import java.io.File
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.TimeUnit

class Adb(private val adbPath: File, private val logger: Logger) {

    private val whitespace = Regex("""\s""")

    fun runningEmulators(): Single<Set<Device>> {
        return ProcessBuilder(adbPath.absolutePath, "devices")
                .toObservable("adb", logger, Observable.never())
                .subscribeOn(Schedulers.io())
                .doOnError { logger.info("adb devices root observable threw") }
                .flatMap { (stdOut, stdErr) ->
                    Observable.merge(
                            stdErr.readLines()
                                    .subscribeOn(Schedulers.io())
                                    .doOnNext { logger.info("stdErr: $it") }
                                    .doOnError { logger.info("stdErr threw: adb devices") }
                                    .never(),
                            stdOut.readLines()
                                    .subscribeOn(Schedulers.io())
                                    .doOnNext { logger.info("stdOut: $it") }
                                    .doOnError { logger.info("stdOut threw: adb devices") }
                                    .onErrorResumeNext { _: Throwable -> Observable.empty<String>() }
                    )
                }
                .map { it.trim() }
                .filter { it.endsWith("offline") || it.endsWith("device") || it.endsWith("unauthorized") }
                .map { it.split(whitespace).let { Device(it[0], Device.Status.fromString(it[1])) } }
                .filter { it.isEmulator }
                .collectInto(mutableSetOf<Device>()) { set, device -> set.add(device) }
                .map { it.toSet() }
    }

    fun kill(device: Device): Completable {
        if (!device.isEmulator)
            return Completable.error(IllegalArgumentException("Attempting to kill something other than an AVD"))

        return Socket("localhost", device.emulatorPort())
                .toObservable(
                        Observable.interval(200, TimeUnit.MILLISECONDS)
                                .map { "ping" }
                                .doOnNext { logger.info("sending ping via idling socket") }
                )
                .subscribeOn(Schedulers.io())
                .doOnNext { logger.info("idling socket received: $it") }
                .ignoreElements()
                .onErrorComplete { it is SocketException }
                .doOnComplete { logger.info("socket was hung up. avd killed") }
                .doOnSubscribe {
                    ProcessBuilder(adbPath.absolutePath, "-s", device.id, "emu", "kill")
                            .toObservable("adb", logger, Observable.never())
                            .subscribeOn(Schedulers.io())
                            .doOnError { logger.info("adb kill threw", it) }
                            .flatMap { (stdOut, stdErr) ->
                                Observable.merge(
                                        stdErr.readLines()
                                                .subscribeOn(Schedulers.io())
                                                .doOnNext { logger.info("stdErr: $it") }
                                                .doOnError { logger.info("stdErr threw: adb kill") }
                                                .never(),
                                        stdOut.readLines()
                                                .subscribeOn(Schedulers.io())
                                                .doOnNext { logger.info("stdOut: $it") }
                                                .doOnError { logger.info("stdOut threw: adb kill") }
                                                .onErrorResumeNext { _: Throwable -> Observable.empty<String>() }
                                )
                            }
                            .subscribe()
                }
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

    fun queryProperty(property: String, device: Device): String {
        return ProcessBuilder(adbPath.absolutePath, "-s", device.id, "shell", "getprop", property)
                .toObservable("adb", logger, Observable.never())
                .subscribeOn(Schedulers.io())
                .doOnError { logger.info("adb shell getprop $property root observable threw") }
                .flatMap { (stdOut, stdErr) ->
                    Observable.merge(
                            stdOut.readLines()
                                    .subscribeOn(Schedulers.io())
                                    .doOnNext { logger.info("stdOut: $it") }
                                    .timeout(2, TimeUnit.SECONDS) {
                                        logger.info("getProp TimedOut. emitting:\"getProp:TimeOut\"")
                                        Observable.just("getProp:TimeOut")
                                    }
                                    .doOnError { logger.info("stdOut threw: adb shell getprop $property") },
                            stdErr.readLines()
                                    .subscribeOn(Schedulers.io())
                                    .doOnNext { logger.info("stdErr: $it") }
                                    .doOnError { logger.info("stdErr threw: adb shell getprop $property") }
                                    .never()
                    )
                }
                .firstOrError()
                .blockingGet()
    }
}