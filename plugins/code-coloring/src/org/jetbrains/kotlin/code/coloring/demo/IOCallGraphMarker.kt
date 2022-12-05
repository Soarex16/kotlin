/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.demo

import org.jetbrains.kotlin.code.coloring.callgraph.CallGraphMarker
import org.jetbrains.kotlin.fir.resolve.dfa.Stack
import org.jetbrains.kotlin.fir.resolve.dfa.stackOf
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

open class IOCallGraphMarker protected constructor(private val safety: IOSafety) : CallGraphMarker<IOCallGraphMarker>() {
    val value: IOSafety
        get() = safety

    companion object {
        private val PACKAGE_FQN = FqName("org.jetbrains.kotlin.code.coloring")
        private val IOSafeClassId = ClassId(PACKAGE_FQN, Name.identifier("CodeWithoutIO"))
        private val IOUnsafeClassId = ClassId(PACKAGE_FQN, Name.identifier("CodeWithIO"))

        fun fromAnnotation(annotationClass: IrClass): IOCallGraphMarker? {
            if (!annotationClass.isAnnotationClass) return null

            return when (annotationClass.classId) {
                IOSafeClassId -> IOCallGraphMarker(IOSafety.Safe)
                IOUnsafeClassId -> IOCallGraphMarker(IOSafety.Unsafe)
                else -> null
            }
        }

        // Top in terms of lattice
        fun default(): IOCallGraphMarker = IOCallGraphMarker(IOSafety.Unknown)
    }

    override fun union(other: IOCallGraphMarker): IOCallGraphMarker = IOCallGraphMarker(safety.combine(other.safety))

    override fun intersect(other: IOCallGraphMarker): IOCallGraphMarker = IOCallGraphMarker(safety.combine(other.safety))

    enum class IOSafety {
        // top
        Unknown {
            override fun combine(other: IOSafety): IOSafety = other
        },

        // bot
        Any {
            override fun combine(other: IOSafety): IOSafety = Any
        },
        Safe {
            override fun combine(other: IOSafety): IOSafety = when (other) {
                Unknown -> Safe
                Safe -> Safe
                Any -> Any
                else -> UnsafeTransitively
            }
        },
        Unsafe {
            override fun combine(other: IOSafety): IOSafety = when (other) {
                Unknown -> Unsafe
                Any -> Any
                else -> UnsafeTransitively
            }
        },
        UnsafeTransitively {
            override fun combine(other: IOSafety): IOSafety = when (other) {
                Any -> Any
                else -> UnsafeTransitively
            }
        };

        abstract fun combine(other: IOSafety): IOSafety
    }
}