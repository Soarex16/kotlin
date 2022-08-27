/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.services

import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

class RuntimeClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    companion object {
        private val classPath = System.getProperty("testRuntime.classpath")
    }

    override fun runtimeClassPaths(module: TestModule): List<File> {
        return classPath.split(":").map(::File)
    }
}