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
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okio.Okio.buffer
import okio.Okio.source
import org.slf4j.Logger
import java.io.File
import java.util.concurrent.TimeUnit

class AvdManager(private val avdManager: File,
                 private val logger: Logger) {

    //avdmanager create avd --name '26_6P_Playstore' --package 'system-images;android-26;google_apis_playstore;x86' --device 'Nexus 6P' --tag 'google_apis_playstore'
    fun createAvd(name: String, sdkKey: String, device: String, options: List<String>): Completable {
        val args = mutableListOf<String>(
                avdManager.absolutePath,
                "create", "avd",
                "--name", name,
                "--package", sdkKey,
                "--device", device)
        args.addAll(options)
        return ProcessBuilder(args).toCompletable("avdmanager", logger)
    }
}

fun ProcessBuilder.toCompletable(name: String, logger: Logger): Completable {
    return Completable.create { emitter ->
        try {
            logger.info("Starting process: ${command().joinToString(separator = " ")}")
            val process = start()
            val disposable = CompositeDisposable()
            val stdOut = buffer(source(process.inputStream))
            disposable.add(stdOut.readLines()
                    .subscribeOn(Schedulers.io())
                    .subscribe { logger.info("stdOut: $it") })
            val stdErr = buffer(source(process.errorStream))
            disposable.add(stdErr.readLines()
                    .subscribeOn(Schedulers.io())
                    .subscribe { logger.error("stdErr: $it") })
            disposable.add(object : Disposable {
                override fun isDisposed(): Boolean {
                    return !process.isAlive
                }

                override fun dispose() {
                    process.destroy()
                    stdOut.close()
                    stdErr.close()
                }
            })
            emitter.setDisposable(disposable)

            val finished = process.waitFor(10, TimeUnit.SECONDS)
            if (!finished) process.destroy()

            val result = if (finished) process.exitValue() else -424242

            if (!emitter.isDisposed) {
                when (result) {
                    0 -> emitter.onComplete()
                    -424242 -> emitter.onError(RuntimeException("$name timed out"))
                    else -> emitter.onError(RuntimeException("$name exited with code $result"))
                }
            }
        } catch (error: Throwable) {
            if (!emitter.isDisposed) {
                emitter.onError(error)
            }
        }
    }
}