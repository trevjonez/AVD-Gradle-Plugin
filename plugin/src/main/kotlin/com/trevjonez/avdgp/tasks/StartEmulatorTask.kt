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
import com.trevjonez.avdgp.rx.readLines
import com.trevjonez.avdgp.rx.toObservable
import com.trevjonez.avdgp.sdktools.Adb
import com.trevjonez.avdgp.sdktools.AvdDeviceNameTransformer
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.TimeUnit

open class StartEmulatorTask : DefaultTask() {
    lateinit var sdkPath: File
    lateinit var configGroup: NamedConfigurationGroup

    var home: File? = null

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
        val args = mutableListOf("/bin/sh", "-c")
        val emuInvocation = mutableListOf("nohup",
                File(sdkPath, "emulator${File.separator}emulator").absolutePath.replace(" ", "\\ "))
                .apply {
                    add("-avd")
                    add(configGroup.escapedName)
                    configGroup.launchOptions.forEach { add(it) }
                }.joinToString(separator = " ")
        args.add(emuInvocation)

        //Launch emulator process
        var processError: Throwable? = null
        ProcessBuilder(args)
                .also { builder ->
                    home?.let {
                        builder.environment().put("HOME", it.absolutePath.replace(" ", "\\ "))
                        builder.environment().put("ANDROID_HOME", File(it, "Android/sdk").absolutePath.replace(" ", "\\ "))
                    }
                }
                .toObservable("emulator", logger, Observable.never())
                .subscribeOn(Schedulers.io())
                .doOnError { logger.info("emulator command threw") }
                .flatMap { (stdOut, stdErr) ->
                    Observable.merge(
                            stdOut.readLines()
                                    .subscribeOn(Schedulers.io())
                                    .doOnNext { logger.info("stdOut: $it") }
                                    .doOnError { logger.info("emulator stdOut threw") },

                            stdErr.readLines()
                                    .subscribeOn(Schedulers.io())
                                    .doOnNext { logger.info("stdErr: $it") }
                                    .doOnError { logger.info("emulator stdErr threw") }
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
    }

    private fun Single<Set<Adb.Device>>.keyedByName() = compose(deviceNameTransformer)
}

