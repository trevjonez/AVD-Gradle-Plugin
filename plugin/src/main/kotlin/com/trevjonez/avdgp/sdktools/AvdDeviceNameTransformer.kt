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

import com.trevjonez.avdgp.rx.doOnFirst
import com.trevjonez.avdgp.rx.toObservable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.SingleTransformer
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.slf4j.Logger
import java.net.Socket

class AvdDeviceNameTransformer(private val logger: Logger)
    : SingleTransformer<Set<Adb.Device>, Map<String, Adb.Device>> {

    override fun apply(upstream: Single<Set<Adb.Device>>): SingleSource<Map<String, Adb.Device>> {
        return upstream.flatMap {
            Observable.fromIterable(it)
                    .flatMapSingle { device ->
                        val sendSubject = PublishSubject.create<String>()
                        Socket("localhost", device.emulatorPort())
                                .toObservable(sendSubject.doOnNext { logger.info("sending: $it") })
                                .subscribeOn(Schedulers.io())
                                .doOnError { logger.info("Socket observable threw") }
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