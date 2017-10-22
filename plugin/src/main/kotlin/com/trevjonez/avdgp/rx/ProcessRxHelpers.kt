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

package com.trevjonez.avdgp.rx

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okio.Okio.buffer
import okio.Okio.source
import org.slf4j.Logger
import java.util.concurrent.TimeUnit

typealias StdOut = okio.BufferedSource
typealias StdErr = okio.BufferedSource

fun ProcessBuilder.toObservable(name: String, logger: Logger, stdIn: Observable<String>): Observable<Pair<StdOut, StdErr>> {
    return Observable.create<Pair<StdOut, StdErr>> { emitter ->
        try {
            logger.info("Starting process: ${command().joinToString(separator = " ")}")
            val process = start()
            val inWriter = process.outputStream.bufferedWriter()
            val disposable = CompositeDisposable()
            emitter.setDisposable(disposable)
            val stdOut = buffer(source(process.inputStream))
            val stdErr = buffer(source(process.errorStream))

            stdIn.observeOn(Schedulers.io())
                    .subscribe {
                        inWriter.write(it)
                        inWriter.newLine()
                        inWriter.flush()
                    } addTo disposable

            object : Disposable {
                var disposed = false
                override fun isDisposed(): Boolean {
                    return disposed
                }

                override fun dispose() {
                    logger.info("Disposing: ${command().joinToString(separator = " ")}")
                    disposed = true
                    inWriter.close()
                }
            } addTo disposable


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

fun ProcessBuilder.toCompletable(name: String, logger: Logger): Completable {
    return Completable.create { emitter ->
        try {
            logger.info("Starting process: ${command().joinToString(separator = " ")}")
            val process = start()
            val disposable = CompositeDisposable()
            emitter.setDisposable(disposable)

            val stdOut = buffer(source(process.inputStream))
            stdOut.readLines()
                    .subscribeOn(Schedulers.io())
                    .subscribe { logger.info("stdOut: $it") } addTo disposable

            val stdErr = buffer(source(process.errorStream))
            stdErr.readLines()
                    .subscribeOn(Schedulers.io())
                    .subscribe { logger.info("stdErr: $it") } addTo disposable

            object : Disposable {
                var disposed = false
                override fun isDisposed() = disposed

                override fun dispose() {
                    logger.info("Disposing: ${command().joinToString(separator = " ")}")
                    disposed = true
                    process.destroy()
                }
            } addTo disposable


            val timeout = 60L
            val finished = process.waitFor(timeout, TimeUnit.SECONDS)
            if (!finished) {
                logger.info("waited $timeout second(s). destroying process")
                process.destroy()
            }

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