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

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okio.Okio
import java.net.Socket

fun Socket.toObservable(input: Observable<String>): Observable<String> {
    return Observable.create { emitter ->
        try {
            val disposable = CompositeDisposable()
            emitter.setDisposable(disposable)

            val out = getOutputStream().bufferedWriter()
            input.observeOn(Schedulers.io()).subscribe {
                out.write(it)
                out.newLine()
                out.flush()
            } addTo disposable

            Okio.buffer(Okio.source(getInputStream()))
                    .readLines()
                    .subscribeOn(Schedulers.io())
                    .subscribe { emitter.onNext(it) } addTo disposable

            object : Disposable {
                override fun isDisposed(): Boolean {
                    return isClosed
                }

                override fun dispose() {
                    close()
                }
            } addTo disposable
        } catch (error: Throwable) {
            if (!emitter.isDisposed) emitter.onError(error)
        }
    }
}