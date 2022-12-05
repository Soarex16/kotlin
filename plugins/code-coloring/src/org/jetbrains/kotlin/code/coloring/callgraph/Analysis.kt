/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.callgraph

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.code.coloring.demo.IOCallGraphMarker
import org.jetbrains.kotlin.fir.resolve.dfa.Stack
import org.jetbrains.kotlin.fir.resolve.dfa.isNotEmpty
import org.jetbrains.kotlin.fir.resolve.dfa.stackOf
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

fun IrValueParameter.isFunctionLike(): Boolean = type.isFunctionOrKFunction() || type.isSuspendFunctionOrKFunction()

fun IrType.getIOMarkers(): List<IOCallGraphMarker> =
    annotations
        .mapNotNull { IOCallGraphMarker.fromAnnotation(it.type.getClass()!!) }
        .distinctBy { it.value }

class FunctionalParameterChecker(
    val callGraph: CallGraph,
    val condensation: DirectedGraphCondensation<IrFunction>,
    val context: IrPluginContext,
    val reporter: IrMessageLogger
) : IrElementVisitorVoid {
    override fun visitFunction(declaration: IrFunction) {
        if (declaration in callGraph) {
            val declarationNode = callGraph[declaration]
            val ownMarker = declarationNode.marker

            // check if safe function accepts only safe lambdas
            if (ownMarker is IOCallGraphMarker && ownMarker.value == IOCallGraphMarker.IOSafety.Safe) {
                for (param in declaration.valueParameters) {
                    if (param.isFunctionLike()) {
                        val ioMarkers = param.type.getIOMarkers()
                        when (ioMarkers.size) {
                            0 -> reporter.report(
                                IrMessageLogger.Severity.ERROR,
                                "Function-like value parameters must be explicitly annotated with @WithoutIO marker",
                                param.location()
                            )

                            1 -> {} // предполагается, что если тип проаннотирован явно, то программист знает, что там на ыafety
                            else -> reporter.report(
                                IrMessageLogger.Severity.WARNING,
                                "Function-like value parameters must have only one io marker",
                                param.location()
                            )
                        }
                    }
                }
            }
        }

        super.visitFunction(declaration)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
        val calleeFunction = expression.symbol.owner
        if (calleeFunction in callGraph) {
            for (argNum in 0 until expression.valueArgumentsCount) {
                val valueParam = calleeFunction.valueParameters[argNum]
                val valueArg = expression.getValueArgument(argNum)!!

                if (valueParam.isFunctionLike()) {
                    val paramMarkers = valueParam.type.getIOMarkers()
                    val argMarkers = valueArg.type.getIOMarkers()

                    if (paramMarkers != argMarkers) {
                        reporter.report(
                            IrMessageLogger.Severity.WARNING,
                            "Passed argument must have same IO marker as function parameter",
                            emptyLocation()
                        )
                    }
                }
            }
        }

        super.visitFunctionAccess(expression)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }
}

class ColorChecker(
    private val callGraph: CallGraph,
    private val condensation: DirectedGraphCondensation<IrFunction>,
    val context: IrPluginContext,
    private val reporter: IrMessageLogger
) : IrElementVisitorVoid {
    override fun visitFunction(declaration: IrFunction) {
        if (declaration in callGraph) {
            val clusterNode = condensation[declaration]
            val declarationNode = callGraph[declaration]
            val propagatedMarker = clusterNode?.marker
            val ownMarker = declarationNode.marker
            if (propagatedMarker is IOCallGraphMarker && ownMarker is IOCallGraphMarker) {
                // какая-то фигня, кажется, что хотим обнаруживать вызовы не только в функциях,
                // явно помеченных как безопасные
                if (ownMarker.value == IOCallGraphMarker.IOSafety.Safe) {
                    when (propagatedMarker.value) {
                        IOCallGraphMarker.IOSafety.Unsafe, // unsafe is not possible as a propagated marker due to the lattice structure
                        IOCallGraphMarker.IOSafety.UnsafeTransitively -> {
                            val trail = (propagatedMarker as IOCallGraphMarkerWithTrail).trail.joinToString("->") { it.name.asString() }
                            // build path of transitive call
                            reporter.report(
                                IrMessageLogger.Severity.ERROR,
                                "Detected illegal call of IO unsafe function: $trail",
                                declaration.location()
                            )
                        }

                        IOCallGraphMarker.IOSafety.Unknown -> {
                            reporter.report(
                                IrMessageLogger.Severity.ERROR,
                                "Cannot infer IO safety for function. Please mark it explicitly with annotation",
                                declaration.location()
                            )
                        }

                        IOCallGraphMarker.IOSafety.Any -> throw IllegalStateException("Color cannot be ${IOCallGraphMarker.IOSafety.Any}")
                        else -> {}
                    }
                }
            }
        }

        super.visitFunction(declaration)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }
}

internal fun IrDeclaration.location(): IrMessageLogger.Location? = this.locationWithOffset(startOffset)

internal fun IrDeclaration.locationWithOffset(offset: Int): IrMessageLogger.Location? = fileOrNull?.location(offset)

internal fun emptyLocation() = IrMessageLogger.Location("", -1, -1)

internal fun IrFile.location(offset: Int): IrMessageLogger.Location {
    val module = module.name
    val fileEntry = fileEntry
    val fileName = fileEntry.name
    val lineNumber = fileEntry.getLineNumber(offset) + 1 // since humans count from 1, not 0
    val columnNumber = fileEntry.getColumnNumber(offset) + 1
    // unsure whether should module name be added here
    return IrMessageLogger.Location("$module @ $fileName", lineNumber, columnNumber)
}

class IOCallGraphMarkerWithTrail(marker: IOCallGraphMarker, val trails: Stack<MutableList<IrFunction>> = stackOf()) :
    IOCallGraphMarker(marker.value) {
    val trail: List<IrFunction>
        get() {
            require(trails.isNotEmpty) { "there is no any collected trail" }
            return trails.top()
        }

    fun startTrail(unsafeFun: IrFunction) = trails.push(mutableListOf(unsafeFun))

    fun appendToTrail(unsafeFun: IrFunction) {
        require(trails.isNotEmpty)
        trails.top().add(unsafeFun)
    }
}

fun propagateMarkers(callGraph: CallGraph, condensation: DirectedGraphCondensation<IrFunction>) {
    /*
    в trail строим путь от плохого узла (помеченного как unsafe), если на очередном шаге встретился новый unsafe,
    то сбрасываем trail и (или) копим заново

    markers combined in following cases:
     - when computing common marker on loop
     - when computing marker based on callees

    lhs     rhs     result
    simple  simple  create trail if rhs unsafe
    simple  trail   can occur only at first iteration of combination, create trail if rhs is unsafe (same as previous)
    trail   simple  append to trail or start new trail if rhs unsafe
    trail   trail   magically combine trails

    TODO: trail collector вынести в отдельную сущность и использовать
     */
    fun combineMarkers(lhs: IOCallGraphMarker, rhs: IOCallGraphMarker, rhsNode: CallGraphNode): IOCallGraphMarker {
        val newMarker = lhs.intersect(rhs)
        return when {
            lhs !is IOCallGraphMarkerWithTrail -> {
                // create trail if rhs is unsafe, drop??? rhs trail
                when (rhs.value) {
                    IOCallGraphMarker.IOSafety.Unsafe -> IOCallGraphMarkerWithTrail(newMarker).apply { startTrail(rhsNode.symbol) }
                    // in case of loop
                    IOCallGraphMarker.IOSafety.UnsafeTransitively -> {
                        val rhsTrails = (rhs as IOCallGraphMarkerWithTrail).trails
                        IOCallGraphMarkerWithTrail(newMarker, rhsTrails) //.apply { appendToTrail(rhsNode.symbol) }
                    }

                    else -> newMarker
                }
            }

            rhs !is IOCallGraphMarkerWithTrail -> {
                // append to trail or start new trail if rhs unsafe
                if (rhs.value == IOCallGraphMarker.IOSafety.Unsafe) {
                    IOCallGraphMarkerWithTrail(newMarker).apply { startTrail(rhsNode.symbol) }
                } else {
                    // TODO: constructor looks weird, refactor
                    IOCallGraphMarkerWithTrail(newMarker, lhs.trails).apply { appendToTrail(rhsNode.symbol) }
                }
            }

            rhs is IOCallGraphMarkerWithTrail -> {
                /*
                 * here we need to join two trails
                 * for ex.:
                 * f_1──►f_2──►f_3─┐
                 *                 ├─►foo
                 *       g_1──►g_2─┘
                 *
                 * and we just drop previous trails and start new one
                 * this behaviour looks appropriate because we will report error on foo
                 * and when user will navigate to foo we highlight invocations of f_3 and g_2 in foo body
                 */
                IOCallGraphMarkerWithTrail(newMarker).apply { startTrail(rhsNode.symbol) }
            }

            else -> error("Unreachable")
        }
    }

    for (multiNode in condensation.topologicalOrder.reversed()) {
        val clusterNodes = multiNode
            .nodes
            .map { callGraph[it] }
            .toSet()

        val externalNodes = clusterNodes
            .flatMap { it.directEdges }
            .map { callGraph[it] }
            .toSet() - clusterNodes

        fun Iterable<CallGraphNode>.commonMarker(): IOCallGraphMarker =
            fold(IOCallGraphMarker.default()) { acc, node ->
                val marker = condensation[node.key]?.marker ?: node.marker
                combineMarkers(acc, marker as IOCallGraphMarker, node)
            }

        // циклы надо как-то отдельно обрабатывать
        val commonLoopMarker = clusterNodes.commonMarker() // potentially redundant
        val markerFromCallees = externalNodes.commonMarker()

        // commonLoopMarker.intersect(markerFromCallees)
        val marker = combineMarkers(commonLoopMarker, markerFromCallees, callGraph[multiNode.nodes.first()])
        multiNode.marker = marker
    }
}

private operator fun DirectedGraphCondensation<IrFunction>.get(key: IrFunction): DirectedGraphMultiNode<*>? {
    return topologicalOrder.find {
        it.nodes.contains(key)
    }
}