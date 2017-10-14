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

package com.trevjonez.avdgp

import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

class BuildDirFolder(private val intermediateDirName: String? = null) : TestRule {

    annotation class UseTemporaryFolder

    private val tempRule = TemporaryFolder()
    private lateinit var directory: File

    var usingTemp = false
    val root: File
        get() {
            return if (usingTemp) tempRule.root else directory
        }

    override fun apply(base: Statement, description: Description): Statement {
        if (description.getAnnotation(UseTemporaryFolder::class.java) != null) {
            usingTemp = true
            return tempRule.apply(base, description)
        }

        return object : Statement() {
            override fun evaluate() {
                val dirName = description.methodName.let {
                    if (!intermediateDirName.isNullOrBlank()) {
                        "$intermediateDirName${File.separator}$it"
                    } else {
                        it
                    }
                }

                directory = File("build", dirName)
                directory.apply {

                    if (exists()) {
                        if (!deleteRecursively()) {
                            throw IllegalStateException("Failed to delete existing directory: $absolutePath")
                        }
                    }

                    if (!mkdirs()) {
                        throw IllegalStateException("Failed to create directory: $absolutePath")
                    }
                }

                base.evaluate()
            }
        }
    }
}
