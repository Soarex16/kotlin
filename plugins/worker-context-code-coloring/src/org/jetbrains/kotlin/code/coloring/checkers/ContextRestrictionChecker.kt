/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.resolved
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

object ContextRestrictionChecker : FirQualifiedAccessExpressionChecker() {
    private val ANNOTATIONS_PACKAGE_FQN = FqName("org.jetbrains.kotlin.code.coloring")
    private val RestrictedContextClassId = ClassId(ANNOTATIONS_PACKAGE_FQN, Name.identifier("RestrictedContext"))

    private val ALLOWED_PACKAGES_ARGUMENT = Name.identifier("allowedPackages")

    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val reference = expression.calleeReference.resolved ?: return
        val symbol = reference.resolvedSymbol as? FirFunctionSymbol ?: return
        val enclosingFunction = findEnclosingFunction(context) ?: return

        val enclosingFunctionContextRestrictions = getContextRestrictions(enclosingFunction.symbol, context.session)
        if (enclosingFunctionContextRestrictions.isEmpty()) return

        val allowedPackages = enclosingFunctionContextRestrictions.flatMap { getAllowedPackages(it) }
        if (allowedPackages.any { symbol.callableId.packageName.toString().startsWith(it) }) return

        fun report() {
            reporter.reportOn(
                expression.source,
                PluginErrors.ILLEGAL_RESTRICTED_FUNCTION_CALL,
                enclosingFunctionContextRestrictions,
                context
            )
        }

        val calleeFunctionContextRestrictions = getContextRestrictions(symbol, context.session)
        if (calleeFunctionContextRestrictions.isEmpty()) {
            report()
            return
        }

        // restrictions(enclosing) ⊆ restrictions(callee)
        val commonRestrictions = calleeFunctionContextRestrictions intersect enclosingFunctionContextRestrictions
        if (commonRestrictions.size != enclosingFunctionContextRestrictions.size) {
            report()
        }
    }

    private fun getAllowedPackages(symbol: FirRegularClassSymbol): List<String> {
        require(symbol.hasAnnotation(RestrictedContextClassId))
        return symbol.getAnnotationByClassId(RestrictedContextClassId)!!.getStringArrayArgument(ALLOWED_PACKAGES_ARGUMENT) ?: emptyList()
    }

    private fun getContextRestrictions(calledDeclarationSymbol: FirCallableSymbol<*>, session: FirSession): Set<FirRegularClassSymbol> {
        return calledDeclarationSymbol.resolvedContextReceivers
            .mapNotNull { it.typeRef.coneType.toRegularClassSymbol(session) }
            .filter { it.hasAnnotation(RestrictedContextClassId) }
            .toSet()
    }

    private fun findEnclosingFunction(context: CheckerContext): FirFunction? {
        return context.containingDeclarations.lastIsInstanceOrNull()
    }
}
