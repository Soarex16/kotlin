/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

object PluginErrors {
    val ILLEGAL_RESTRICTED_FUNCTION_CALL by error1<PsiElement, Collection<FirRegularClassSymbol>>(SourceElementPositioningStrategies.WHOLE_ELEMENT)

    object PluginErrorMessages : BaseDiagnosticRendererFactory() {
        override val MAP = KtDiagnosticFactoryToRendererMap("CodeColoring").also { map ->
            map.put(
                ILLEGAL_RESTRICTED_FUNCTION_CALL,
                "Cannot call a function that is not marked with a restrictions ''{0}''",
                KtDiagnosticRenderers.COLLECTION(FirDiagnosticRenderers.DECLARATION_NAME)
            )
        }

    }

    init {
        RootDiagnosticRendererFactory.registerFactory(PluginErrorMessages)
    }
}
