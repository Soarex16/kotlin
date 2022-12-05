/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.callgraph

import org.jetbrains.kotlin.code.coloring.DummyMessageReporter
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class CallGraphBuilder {
    companion object {
        fun processModuleFragment(module: IrModuleFragment): Pair<CallGraph, DirectedGraphCondensation<IrFunction>> {
            val graphBuilder = RawCallGraphBuilder(DummyMessageReporter())
            graphBuilder.visitModuleFragment(module)

            return Pair(graphBuilder.callGraph, DirectedGraphCondensationBuilder(graphBuilder.callGraph).build())
        }
    }
}