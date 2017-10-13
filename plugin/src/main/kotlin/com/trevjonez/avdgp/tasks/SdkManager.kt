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

import io.reactivex.Observable
import java.io.BufferedReader
import java.io.File

class SdkManager(private val sdkManager: File) {
    fun install(sdkKey: String): Pair<Observable<String>, (String) -> Unit> {
        val process = ProcessBuilder(sdkManager.absolutePath, sdkKey).start()
        val outputWriter = process.outputStream.bufferedWriter()
        return process.inputStream
                .bufferedReader()
                .toObservable()
                .doOnEach { println("$it") }
                .doOnDispose { outputWriter.close() } to { input ->
            outputWriter.apply {
                write(input); newLine(); flush()
            }
        }
    }

    fun BufferedReader.toObservable(): Observable<String> {
        return Observable.create<String> { emitter ->
            try {
                val stream = this.lines().iterator()
                while (true) {
                    if (stream.hasNext()) {
                        val next = stream.next()
                        if (!emitter.isDisposed) {
                            emitter.onNext(next)
                        }
                    }

                    if (emitter.isDisposed) {
                        break
                    }

                    if (!stream.hasNext()) {
                        Thread.sleep(10)
                    }
                }
            } catch (error: Throwable) {
                if (!emitter.isDisposed) {
                    emitter.onError(error)
                }
            }
        }
    }
}