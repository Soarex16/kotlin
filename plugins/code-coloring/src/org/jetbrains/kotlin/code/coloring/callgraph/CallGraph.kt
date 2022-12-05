/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.callgraph

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression

class CallGraphNode(val graph: CallGraph, val symbol: IrFunction, var marker: CallGraphMarker<*>) : DirectedGraphNode<IrFunction> {

    override val key get() = symbol

    override val directEdges: List<IrFunction> by lazy {
        graph.directEdges[symbol]!!.callSites
            .map { it.callee }
            .filter { graph.directEdges.containsKey(it) }
    }

    override val reversedEdges: List<IrFunction> by lazy {
        graph.reversedEdges[symbol]!!
    }

    class CallSite(val call: IrFunctionAccessExpression, val callee: IrFunction)

    val callSites = mutableListOf<CallSite>()
}

class CallGraph(
    val directEdges: Map<IrFunction, CallGraphNode>,
    val reversedEdges: Map<IrFunction, MutableList<IrFunction>>
) : DirectedGraph<IrFunction, CallGraphNode> {

    override val nodes get() = directEdges.values

    override fun contains(key: IrFunction): Boolean = key in directEdges

    override fun get(key: IrFunction) = directEdges[key]!!

    fun addEdge(caller: IrFunction, callSite: CallGraphNode.CallSite) {
        directEdges[caller]!!.callSites += callSite
    }

    fun addReversedEdge(caller: IrFunction, callee: IrFunction) {
        reversedEdges[callee]!!.add(caller)
    }
}