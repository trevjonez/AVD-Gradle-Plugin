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

import com.trevjonez.avdgp.dsl.NamedConfigurationGroup
import com.trevjonez.avdgp.rx.doOnFirst
import com.trevjonez.avdgp.rx.readLines
import com.trevjonez.avdgp.rx.toObservable
import com.trevjonez.avdgp.sdktools.Adb
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.SingleTransformer
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import java.io.File
import java.net.Socket
import java.util.concurrent.TimeUnit

open class StartEmulatorTask : DefaultTask() {
    lateinit var sdkPath: File
    lateinit var configGroup: NamedConfigurationGroup

    var avdPath: File? = null

    private val adb by lazy {
        Adb(File(sdkPath, "platform-tools${File.separator}adb"), logger)
    }

    private val deviceNameTransformer by lazy {
        AvdDeviceNameTransformer(logger)
    }

    init {
        outputs.upToDateWhen {
            adb.runningEmulators()
                    .keyedByName()
                    .blockingGet()
                    .any { it.key == configGroup.escapedName }
        }
    }

    @TaskAction
    fun invoke() {
        val args = mutableListOf<String>("nohup", File(sdkPath, "emulator${File.separator}emulator").absolutePath).apply {
            add("-avd")
            add(configGroup.escapedName)
            configGroup.launchOptions.forEach { add(it) }
        }
        //Launch emulator process
        var processError: Throwable? = null
        val processDisposable = ProcessBuilder(args)
                .also { builder ->
                    builder.environment().put("ANDROID_SDK_ROOT", sdkPath.absolutePath)
                    avdPath?.let { builder.environment().put("ANDROID_AVD_HOME", it.absolutePath) }
                }
                .toObservable("nohup emulator", logger, Observable.never())
                .subscribeOn(Schedulers.io())
                .flatMap { (stdOut, stdErr) ->
                    Observable.merge(
                            stdOut.readLines()
                                    .subscribeOn(Schedulers.io())
                                    .doOnNext { logger.info("stdOut: $it") },

                            stdErr.readLines()
                                    .subscribeOn(Schedulers.io())
                                    .doOnNext { logger.info("stdErr: $it") }
                    )
                }
                .subscribe({}, { processError = it })

        //Read running emulators until ours shows up so we have the port to query
        val device = Observable.interval(2, TimeUnit.SECONDS)
                .switchMapSingle { adb.runningEmulators().keyedByName() }
                .doOnNext { processError?.let { throw it } }
                .skipWhile { !it.keys.contains(configGroup.escapedName) || it[configGroup.escapedName]!!.status != Adb.Device.Status.ONLINE }
                .firstOrError()
                .timeout(30, TimeUnit.SECONDS)
                .map { it[configGroup.escapedName]!! }
                .blockingGet()

        //query for the boot anim property to detect when the system ui is ready
        Observable.interval(2, TimeUnit.SECONDS)
                .doOnNext { processError?.let { throw it } }
                .map { adb.queryProperty("init.svc.bootanim", device) }
                .skipWhile { it != "stopped" }
                .firstOrError()
                .toCompletable()
                .blockingAwait()

        processDisposable.dispose()
    }

    private fun Single<Set<Adb.Device>>.keyedByName() = compose(deviceNameTransformer)

    private class AvdDeviceNameTransformer(private val logger: Logger)
        : SingleTransformer<Set<Adb.Device>, Map<String, Adb.Device>> {

        override fun apply(upstream: Single<Set<Adb.Device>>): SingleSource<Map<String, Adb.Device>> {
            return upstream.flatMap {
                Observable.fromIterable(it)
                        .flatMapSingle { device ->
                            val sendSubject = PublishSubject.create<String>()
                            Socket("localhost", device.emulatorPort())
                                    .toObservable(sendSubject.doOnNext { logger.info("sending: $it") })
                                    .subscribeOn(Schedulers.io())
                                    .skipWhile { it.trim() != "OK" }
                                    .doOnFirst { sendSubject.onNext("avd name") }
                                    .filter { it != "OK" }
                                    .firstOrError()
                                    .doOnSuccess { logger.info("devName: $it") }
                                    .map { it to device }
                        }
                        .toList()
                        .map { it.toMap() }
            }
        }
    }
}

