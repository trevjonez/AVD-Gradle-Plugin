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
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okio.BufferedSource
import okio.Okio
import java.io.IOException

typealias StdOut = okio.BufferedSource
typealias StdErr = okio.BufferedSource

fun BufferedSource.drain(): Observable<Char> {
    return Observable.create { emitter ->
        try {
            var skipped = 0
            while (true) {
                val next = try {
                    readByte().toChar()
                } catch (error: IOException) {
                    null
                }

                if (!emitter.isDisposed && next != null) {
                    emitter.onNext(next)
                } else {
                    skipped++
                }

                if (skipped > 10) break
                if (emitter.isDisposed) break
            }

            if (!emitter.isDisposed) emitter.onComplete()
        } catch (error: Throwable) {
            if (!emitter.isDisposed) emitter.onError(error)
        }
    }
}

fun BufferedSource.readLines(): Observable<String> {
    return Observable.create { emitter ->
        try {
            var next = readUtf8Line()
            while (next != null) {
                if (!emitter.isDisposed) emitter.onNext(next)
                next = readUtf8Line()
            }
            if (!emitter.isDisposed) emitter.onComplete()
        } catch (error: Throwable) {
            if (!emitter.isDisposed) emitter.onError(error)
        }
    }
}

fun <T> Observable<T>.never(): Observable<T> {
    return filter { false }
}

fun ProcessBuilder.toObservable(name: String, logger: org.slf4j.Logger, stdIn: io.reactivex.Observable<String>): io.reactivex.Observable<Pair<com.trevjonez.avdgp.tasks.StdOut, com.trevjonez.avdgp.tasks.StdErr>> {
    return Observable.create<Pair<StdOut, com.trevjonez.avdgp.tasks.StdErr>> { emitter ->
        try {
            logger.info("Starting process: ${command().joinToString(separator = " ")}")
            val process = start()
            val inWriter = process.outputStream.bufferedWriter()
            val disposable = CompositeDisposable()
            val stdOut = Okio.buffer(Okio.source(process.inputStream))
            val stdErr = Okio.buffer(Okio.source(process.errorStream))

            disposable.add(stdIn.observeOn(Schedulers.io())
                    .subscribe {
                        inWriter.write(it)
                        inWriter.newLine()
                        inWriter.flush()
                    })

            disposable.add(object : Disposable {
                override fun isDisposed(): Boolean {
                    return !process.isAlive
                }

                override fun dispose() {
                    process.destroy()
                    stdOut.close()
                    stdErr.close()
                    inWriter.close()
                }
            })

            emitter.setDisposable(disposable)

            if (!emitter.isDisposed) {
                emitter.onNext(stdOut to stdErr)
            }

            val result = process.waitFor()
            if (!emitter.isDisposed) {
                if (result == 0)
                    emitter.onComplete()
                else
                    emitter.onError(RuntimeException("$name exited with code $result"))
            }
        } catch (error: Throwable) {
            if (!emitter.isDisposed) {
                emitter.onError(error)
            }
        }
    }
}