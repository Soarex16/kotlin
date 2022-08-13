/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ILLEGAL_ARGUMENT_IN_WORKER_ANNOTATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INAPPLICABLE_TARGET_WORKER_FUNCTION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.WORKER_FUNCTION_ADDITIONAL_ARGUMENTS_NOT_ALLOWED
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.scopes.impl.hasTypeOf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

private val WEB_WORKER_ANNOTATION = ClassId.fromString("kotlinx/webworkers/annotations/WebWorker")
private val DEDICATED_WORKER_GLOBAL_SCOPE = ClassId.fromString("org/w3c/dom/DedicatedWorkerGlobalScope")
private val WORKER_ID_PARAM = Name.identifier("workerId")

object FirWorkerAnnotatedJsDeclarationChecker : FirFunctionChecker() {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val workerAnnotation = declaration.getAnnotationByClassId(WEB_WORKER_ANNOTATION) ?: return

        if (declaration !is FirSimpleFunction || declaration.dispatchReceiverType != null) {
            reporter.reportOn(
                declaration.source,
                INAPPLICABLE_TARGET_WORKER_FUNCTION,
                context
            )
        }

        val workerId = workerAnnotation.getStringArgument(WORKER_ID_PARAM)
        if (workerId.isNullOrBlank()) {
            reporter.reportOn(
                declaration.source,
                ILLEGAL_ARGUMENT_IN_WORKER_ANNOTATION,
                context
            )
        }

        val params = declaration.valueParameters
        if (params.size != 1 || !params.single().hasTypeOf(DEDICATED_WORKER_GLOBAL_SCOPE, true)) {
            reporter.reportOn(
                declaration.source,
                WORKER_FUNCTION_ADDITIONAL_ARGUMENTS_NOT_ALLOWED,
                context
            )
        }
    }
}
