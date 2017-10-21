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
import io.reactivex.disposables.Disposable
import okio.BufferedSource
import java.io.IOException

fun BufferedSource.drain(): Observable<Char> {
    return Observable.create { emitter ->
        try {
            emitter.setDisposable(object : Disposable {
                override fun isDisposed(): Boolean {
                    return false
                }

                override fun dispose() {
                    close()
                }
            })
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
            if (error is IllegalStateException && error.message?.contains("closed") == true) {
                if (!emitter.isDisposed) emitter.onComplete()
            } else if (error is IOException && error.message?.contains("Stream closed") == true) {
                if (!emitter.isDisposed) emitter.onComplete()
            } else if (!emitter.isDisposed) emitter.onError(error)
        }
    }
}

fun BufferedSource.readLines(): Observable<String> {
    return Observable.create { emitter ->
        try {
            emitter.setDisposable(object : Disposable {
                override fun isDisposed(): Boolean {
                    return false
                }

                override fun dispose() {
                    close()
                }
            })
            var next = readUtf8Line()
            while (next != null) {
                if (!emitter.isDisposed) emitter.onNext(next)
                next = readUtf8Line()
            }
            if (!emitter.isDisposed) emitter.onComplete()
        } catch (error: Throwable) {
            if (error is IllegalStateException && error.message?.contains("closed") == true) {
                if (!emitter.isDisposed) emitter.onComplete()
            } else if (error is IOException && error.message?.contains("Stream closed") == true) {
                if (!emitter.isDisposed) emitter.onComplete()
            } else if (!emitter.isDisposed) emitter.onError(error)
        }
    }
}