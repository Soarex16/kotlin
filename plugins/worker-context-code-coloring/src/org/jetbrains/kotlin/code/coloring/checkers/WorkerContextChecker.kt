/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.resolved
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.hasAnnotation
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

object WorkerContextChecker : FirQualifiedAccessExpressionChecker() {
    private val ANNOTATIONS_PACKAGE_FQN = FqName("org.jetbrains.kotlin.code.coloring")
    private val RestrictedContextClassId = ClassId(ANNOTATIONS_PACKAGE_FQN, Name.identifier("RestrictedContext"))

    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val reference = expression.calleeReference.resolved ?: return
        val symbol = reference.resolvedSymbol as? FirCallableSymbol ?: return
        val enclosingFunction = findEnclosingFunction(context) ?: return
        //val contextRestrictions = getContextRestrictions(enclosingFunction.symbol)
        if (!checkRestrictsSuspension(expression, enclosingFunction, symbol, context)) {
            reporter.reportOn(expression.source, PluginErrors.TEST_ERROR, context)
        }
        //symbol.resolvedContextReceivers
    }

    private fun getContextRestrictions(calledDeclarationSymbol: FirCallableSymbol<*>): List<FirContextReceiver> {
        return calledDeclarationSymbol.resolvedContextReceivers.filter {
            it.typeRef.coneType.hasAnnotation(RestrictedContextClassId.asSingleFqName())
            //  .getAnnotationByClassId(RestrictedContextClassId) != null
        }
    }

    private fun checkRestrictsSuspension(
        expression: FirQualifiedAccessExpression,
        enclosingSuspendFunction: FirFunction,
        calledDeclarationSymbol: FirCallableSymbol<*>,
        context: CheckerContext
    ): Boolean {
        return true
//        val session = context.session
//
//        val enclosingSuspendFunctionDispatchReceiverOwnerSymbol =
//            (enclosingSuspendFunction.dispatchReceiverType as? ConeClassLikeType)?.lookupTag?.toFirRegularClassSymbol(session)
//        val enclosingSuspendFunctionExtensionReceiverOwnerSymbol = enclosingSuspendFunction.takeIf { it.receiverTypeRef != null }?.symbol
//
//        val (dispatchReceiverExpression, extensionReceiverExpression, extensionReceiverParameterType) =
//            expression.computeReceiversInfo(session, calledDeclarationSymbol)
//
//        for (receiverExpression in listOfNotNull(dispatchReceiverExpression, extensionReceiverExpression)) {
//            if (!receiverExpression.typeRef.coneType.isRestrictSuspensionReceiver(session)) continue
//            if (sameInstanceOfReceiver(receiverExpression, enclosingSuspendFunctionDispatchReceiverOwnerSymbol)) continue
//            if (sameInstanceOfReceiver(receiverExpression, enclosingSuspendFunctionExtensionReceiverOwnerSymbol)) continue
//
//            return false
//        }
//
//        if (enclosingSuspendFunctionExtensionReceiverOwnerSymbol?.resolvedReceiverTypeRef?.coneType?.isRestrictSuspensionReceiver(session) != true) {
//            return true
//        }
//
//        if (sameInstanceOfReceiver(dispatchReceiverExpression, enclosingSuspendFunctionExtensionReceiverOwnerSymbol)) {
//            return true
//        }
//
//        if (sameInstanceOfReceiver(extensionReceiverExpression, enclosingSuspendFunctionExtensionReceiverOwnerSymbol)) {
//            if (extensionReceiverParameterType?.isRestrictSuspensionReceiver(session) == true) {
//                return true
//            }
//        }
//        return false
    }

    private fun findEnclosingFunction(context: CheckerContext): FirFunction? {
        return context.containingDeclarations.lastIsInstanceOrNull()
    }
}
