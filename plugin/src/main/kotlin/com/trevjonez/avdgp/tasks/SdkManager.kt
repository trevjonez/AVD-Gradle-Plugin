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
import io.reactivex.schedulers.Schedulers
import okio.BufferedSource
import okio.Okio
import org.gradle.api.logging.Logger
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SdkManager(private val sdkManager: File, private val logger: Logger) {

    enum class LicenseType {
        Sdk, SdkPreview;

        companion object {
            @JvmStatic
            fun fromStdOut(stdOut: String): LicenseType {
                return when {
                    stdOut.contains("License android-sdk-license:") -> Sdk
                    stdOut.contains("License android-sdk-preview-license:") -> SdkPreview
                    else -> throw IllegalArgumentException("No matching stdOut title:\n $stdOut")
                }
            }
        }
    }

    private val licenseHeaderRegex = Regex("""License android-sdk-(preview-)?license:""")

    fun install(sdkKey: String): Pair<Observable<InstallStatus>, (String) -> Unit> {
        logger.info("Attempting install of $sdkKey with sdkmanager $sdkManager")
        val process = ProcessBuilder(sdkManager.absolutePath, sdkKey).start()
        val outputWriter = process.outputStream.bufferedWriter()

        return Observable.merge(process.inputStream.toBufferedSource()
                                        .drain()
                                        .subscribeOn(Schedulers.io())
                                        .scan<Line>(Line.Pending("")) { last, next ->
                                            if (last is Line.Done) {
                                                Line.make("", next)
                                            } else {
                                                Line.make(last.value, next)
                                            }
                                        }
                                        .ofType(Line.Done::class.java)
                                        .map { it.value.trimEnd() }
                                        .doOnNext { logger.info(it) },
                                process.errorStream.toBufferedSource()
                                        .readLines()
                                        .doOnNext(logger::info)
                                        .subscribeOn(Schedulers.io())
                                        .doOnNext { if (it.contains("Failed to find package")) throw Error.PackageNotFound(sdkKey) }
                                        .doOnNext { if (it.contains("Failed to create SDK root dir")) throw Error.Unknown(it) }
                                        .never(),
                                process.completionObservable<String>()
                                        .subscribeOn(Schedulers.io()))
                .scan<InstallStatus>(InstallStatus.InFlight("")) { last, next ->
                    if (next.contains("Usage: ")) {
                        throw Error.InvalidInvocation()
                    }

                    if (last is InstallStatus.PrintingLicense && next.contains("Accept? (y/N):")) {
                        InstallStatus.AwaitingLicense(next, last.licenseType)
                    } else if (last is InstallStatus.AwaitingLicense) {
                        InstallStatus.InFlight(next)
                    } else if (next.matches(licenseHeaderRegex)) {
                        InstallStatus.PrintingLicense(next)
                    } else {
                        last.collect(next)
                    }
                }
                .doOnDispose {
                    outputWriter.close()
                    process.destroy()
                }
                .to({ input: String ->
                        logger.info("received callback with: $input")
                        outputWriter.apply {
                            write(input); newLine(); flush()
                        }
                        Unit
                    })
    }

    sealed class InstallStatus {
        abstract val stdOut: String
        abstract fun collect(nextLine: String): InstallStatus

        data class InFlight(override val stdOut: String) : InstallStatus() {
            override fun collect(nextLine: String)
                    = copy(stdOut = "$stdOut\n$nextLine")
        }

        data class PrintingLicense(override val stdOut: String) : InstallStatus() {
            override fun collect(nextLine: String)
                    = copy(stdOut = "$stdOut\n$nextLine")

            val licenseType = LicenseType.fromStdOut(stdOut)
        }

        data class AwaitingLicense(override val stdOut: String, val licenseType: LicenseType) : InstallStatus() {
            override fun collect(nextLine: String): InstallStatus {
                throw UnsupportedOperationException("Don't collect on awaiting license")
            }
        }
    }

    sealed class Error(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
        class PackageNotFound(packageId: String) : Error("Failed to find package $packageId")
        class InvalidInvocation : Error("Bad cli tool invocation. Run with --info and report to `https://github.com/trevjonez/AVD-Gradle-Plugin/issues/new`")
        class Unknown(message: String) : Error(message)
    }

    sealed class Line {
        abstract val value: String

        data class Pending(override val value: String) : Line()
        data class Done(override val value: String) : Line()

        companion object {

            fun make(last: String, next: Char): Line {
                return if (next == '\n' || last.contains("Accept? (y/N):")) Done(last)
                else Pending(last + next)
            }
        }
    }

    fun InputStream.toBufferedSource(): BufferedSource {
        return Okio.buffer(Okio.source(this))
    }

    fun BufferedSource.drain(): Observable<Char> {
        return Observable.create { emitter ->
            try {
                while (true) {
                    val next = try {
                        readByte().toChar()
                    } catch (error: IOException) {
                        null
                    }

                    if (!emitter.isDisposed && next != null) {
                        emitter.onNext(next)
                    }
                }
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
                    logger.info(next.toString())
                }
                if (!emitter.isDisposed) emitter.onComplete()
            } catch (error: Throwable) {
                if (!emitter.isDisposed) emitter.onError(error)
            }
        }
    }

    fun <T> Process.completionObservable(): Observable<T> {
        return Observable.create { emitter ->
            val returnValue = try {
                waitFor()
            } catch (ignore: InterruptedException) {
                0
            }

            if (!emitter.isDisposed) {
                if (returnValue == 0) emitter.onComplete()
                else emitter.onError(RuntimeException("sdkmanager exited with code: $returnValue"))
            }
        }
    }
}

private fun <T> Observable<T>.never(): Observable<T> {
    return filter { false }
}
