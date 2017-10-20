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

import com.trevjonez.avdgp.dsl.ProxyConfig
import com.trevjonez.avdgp.rx.drain
import com.trevjonez.avdgp.rx.never
import com.trevjonez.avdgp.rx.readLines
import com.trevjonez.avdgp.rx.toObservable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.slf4j.Logger
import java.io.File

class SdkManager(private val sdkManager: File,
                 private val logger: Logger,
                 private val proxyConfig: ProxyConfig?,
                 private val noHttps: Boolean) {

    companion object {
        private val licenseHeaderRegex = Regex("""License android-sdk-(preview-)?license:""")
    }

    fun install(sdkKey: String): Pair<Observable<InstallStatus>, (String) -> Unit> {
        val args = mutableListOf<String>(sdkManager.absolutePath)
        if (proxyConfig != null) {
            args.add("--proxy=${proxyConfig.type}")
            args.add("--proxy_host=${proxyConfig.host}")
            args.add("--proxy_port=${proxyConfig.port}")
        }
        if (noHttps) {
            args.add("--no_https")
        }
        args.add(sdkKey)

        val inSubject = PublishSubject.create<String>()
        return ProcessBuilder(args)
                .toObservable("sdkmanager", logger, inSubject.doOnEach { logger.info("stdIn: $it") })
                .doOnError { logger.info("sdkmanager install threw") }
                .subscribeOn(Schedulers.io())
                .flatMap { (stdOut, stdErr) ->
                    Observable.merge(
                            stdOut.drain()
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
                                    .doOnNext { logger.info("stdOut: $it") }
                                    .doOnError { logger.info("stdOut threw: sdkmanager install") },

                            stdErr.readLines()
                                    .subscribeOn(Schedulers.io())
                                    .doOnNext { logger.info("stdErr: $it") }
                                    .doOnNext { if (it.contains("Failed to find package")) throw Error.PackageNotFound(sdkKey) }
                                    .doOnNext { if (it.contains("Failed to create SDK root dir")) throw Error.Unknown(it) }
                                    .doOnError { logger.info("stdErr threw: sdkmanager install") }
                                    .never()
                    )
                }
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
                .to { it: String -> inSubject.onNext(it) }
    }

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

    private sealed class Line {
        abstract val value: String

        data class Pending(override val value: String) : Line()
        data class Done(override val value: String) : Line()

        companion object {
            fun make(last: String, next: Char): Line {
                val line = last + next
                return if (next == '\n' || next == '\r' || last.contains("Accept? (y/N):")) Done(last)
                else Pending(line)
            }
        }
    }
}

