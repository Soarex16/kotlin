/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.code.coloring.callgraph.CallGraphBuilder
import org.jetbrains.kotlin.code.coloring.callgraph.ColorChecker
import org.jetbrains.kotlin.code.coloring.callgraph.FunctionalParameterChecker
import org.jetbrains.kotlin.code.coloring.callgraph.propagateMarkers
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class CallGraphGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val (callGraph, condensation) = CallGraphBuilder.processModuleFragment(moduleFragment)
        propagateMarkers(callGraph, condensation)

        // Not supported in K2, need to find workaround
//        val reporter = pluginContext.createDiagnosticReporter("CODE_COLORING")
        val reporter = DummyMessageReporter()

        val colorChecker = ColorChecker(callGraph, condensation, pluginContext, reporter)
        val paramChecker = FunctionalParameterChecker(callGraph, condensation, pluginContext, reporter)

        val checkers = listOf(colorChecker, paramChecker)
        for (checker in checkers) {
            moduleFragment.acceptVoid(checker)
        }
    }
}

