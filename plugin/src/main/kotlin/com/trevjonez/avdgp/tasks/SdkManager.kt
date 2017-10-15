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

import com.trevjonez.avdgp.dsl.ProxyConfig
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import okio.BufferedSource
import okio.Okio
import org.slf4j.Logger
import java.io.File
import java.io.IOException
import java.io.InputStream

private val licenseHeaderRegex = Regex("""License android-sdk-(preview-)?license:""")

class SdkManager(private val sdkManager: File,
                 private val logger: Logger,
                 private val proxyConfig: ProxyConfig?,
                 private val noHttps: Boolean) {

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


    fun install(sdkKey: String): Pair<Observable<InstallStatus>, (String) -> Unit> {
        val args = mutableListOf<String>()
        if (proxyConfig != null) {
            args.add("--proxy=${proxyConfig.type}")
            args.add("--proxy_host=${proxyConfig.host}")
            args.add("--proxy_port=${proxyConfig.port}")
        }

        if (noHttps) {
            args.add("--no_https")
        }

        args.add(sdkKey)

        val processBuilder = ProcessBuilder(sdkManager.absolutePath, *args.toTypedArray())
        logger.info("Attempting install: ${processBuilder.command().joinToString(separator = " ")}")

        val process = processBuilder.start()
        val outputWriter = process.outputStream.bufferedWriter()

        return Observable.merge(
                process.inputStream.toBufferedSource()
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
                        .distinctUntilChanged()
                        .doOnNext { logger.info("stdOut: $it") },

                process.errorStream.toBufferedSource()
                        .readLines()
                        .doOnNext { logger.error("stdErr: $it") }
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
                    } else if (next.matches(licenseHeaderRegex)) {
                        InstallStatus.PrintingLicense(next, LicenseType.fromStdOut(next))
                    } else if (last is InstallStatus.PrintingLicense) {
                        InstallStatus.PrintingLicense(next, last.licenseType)
                    } else {
                        InstallStatus.InFlight(next)
                    }
                }
                .doOnDispose {
                    outputWriter.close()
                    process.destroy()
                }
                .to({ input: String ->
                    logger.info("stdIn: $input")
                    outputWriter.apply {
                        write(input); newLine(); flush()
                    }
                    Unit
                })
    }

    sealed class InstallStatus {
        abstract val stdOut: String

        data class InFlight(override val stdOut: String) : InstallStatus()

        data class PrintingLicense(override val stdOut: String, val licenseType: LicenseType) : InstallStatus()

        data class AwaitingLicense(override val stdOut: String, val licenseType: LicenseType) : InstallStatus()
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
                val line = last + next
                return if (line == "done") Done(line)
                else if (next == '\n' || next == '\r' || last.contains("Accept? (y/N):")) Done(last)
                else Pending(line)
            }
        }
    }

    fun InputStream.toBufferedSource(): BufferedSource {
        return Okio.buffer(Okio.source(this))
    }

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
