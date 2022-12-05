/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.callgraph

import org.jetbrains.kotlin.code.coloring.demo.IOCallGraphMarker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.allOverridden
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class RawCallGraphBuilder(val reporter: IrMessageLogger) : IrElementVisitorVoid {
    private val directEdges = mutableMapOf<IrFunction, CallGraphNode>()
    private val reversedEdges = mutableMapOf<IrFunction, MutableList<IrFunction>>()
    val callGraph = CallGraph(directEdges, reversedEdges)

    private var currentCaller: IrFunction? = null

    private fun addNode(symbol: IrFunction, marker: IOCallGraphMarker) {
        directEdges[symbol] = CallGraphNode(callGraph, symbol, marker)
        reversedEdges[symbol] = mutableListOf()
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
        val calleeFunction = expression.symbol.owner
        if (shouldSkipFunction(calleeFunction)) {
            return
        }

        if (calleeFunction !in directEdges) {
            visitFunction(calleeFunction)
        }

        val callSite = CallGraphNode.CallSite(expression, calleeFunction)
        callGraph.addEdge(currentCaller!!, callSite)
        callGraph.addReversedEdge(currentCaller!!, calleeFunction)

        super.visitFunctionAccess(expression)
    }

    private fun shouldSkipFunction(declaration: IrFunction): Boolean {
        return declaration.isFakeOverride || isBuiltIn(declaration) || declaration is IrConstructor
    }

    // TODO: в проекте может быть куча equals, hashCode и прочего из Object. Надо это как-то учитывать и оптимизировать в будущем
    override fun visitFunction(declaration: IrFunction) {
        // currently we skip constructors
        if (declaration in directEdges || shouldSkipFunction(declaration)) {
            return
        }

        val marker = when (declaration) {
            is IrSimpleFunction -> {
                val ownMarker = getGraphMarker(declaration)
                if (ownMarker != null) {
                    ownMarker
                } else {
                    val inheritedMarkers = getInheritedGraphMarkers(declaration).distinctBy { it.value }
                    when (inheritedMarkers.size) {
                        0 -> IOCallGraphMarker.default()
                        1 -> inheritedMarkers.single()
                        else -> {
                            reporter.report(
                                IrMessageLogger.Severity.WARNING,
                                "This function should have explicit coloring",
                                declaration.location()
                            )
                            IOCallGraphMarker.default()
                        }
                    }
                }
            }

            is IrConstructor -> {
                getGraphMarker(declaration) ?: IOCallGraphMarker.default()
            }

            else -> error("unreachable")
        }

        addNode(declaration, marker)

        val prevCaller = currentCaller
        currentCaller = declaration
        super.visitFunction(declaration)
        currentCaller = prevCaller
    }

    private fun getGraphMarker(declaration: IrFunction): IOCallGraphMarker? {
        return declaration.annotations.firstNotNullOfOrNull { ir ->
            val annotationType = ir.type.getClass() ?: return@firstNotNullOfOrNull null
            IOCallGraphMarker.fromAnnotation(annotationType)
        }
    }

    private fun getInheritedGraphMarkers(declaration: IrSimpleFunction): List<IOCallGraphMarker> {
        val overridens = declaration.allOverridden()
        return overridens
            .reversed() // Если у кого-то overriddenSymbols.size > 1, то результат не определен
            .mapNotNull(::getGraphMarker)
    }

    private fun isBuiltIn(declaration: IrFunction): Boolean {
        val declarationOrigin = declaration.origin
        return declarationOrigin is IrDeclarationOriginImpl && declarationOrigin.name == "BUILTIN_CLASS_METHOD"
    }
}