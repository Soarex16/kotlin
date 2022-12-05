/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.runners

import org.jetbrains.kotlin.code.coloring.callgraph.CallGraph
import org.jetbrains.kotlin.code.coloring.callgraph.CallGraphMarker
import org.jetbrains.kotlin.code.coloring.callgraph.DirectedGraphCondensation
import org.jetbrains.kotlin.code.coloring.demo.IOCallGraphMarker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr.mangleString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.utils.Printer

class CallGraphRenderer(
    private val callGraph: CallGraph,
    private val callGraphCondensation: DirectedGraphCondensation<IrFunction>,
    builder: StringBuilder
) {
    companion object {
        private const val EDGE = " -> "
    }

    private val printer = Printer(builder)

    private val nodeNames = mutableMapOf<IrFunction, String>()

    fun renderDotGraph() {
        printer
            .println("digraph call_graph {")
            .pushIndent()
            .println()

        printer.renderNodes()
        printer.renderEdges()

        printer
            .popIndent()
            .println("}")
    }

    private fun Printer.renderEdges() {
        val edges = mutableListOf<String>()

        for ((caller, callees) in callGraph.reversedEdges) {
            for (callee in callees) {
                edges.add("${nodeNames[callee]}${EDGE}${nodeNames[caller]};")
            }
        }

        for (edge in edges.sorted()) {
            println(edge)
        }
    }

    private fun Printer.renderNodes() {
        var clusterNum = 0
        for (multiNode in callGraphCondensation.topologicalOrder) {
            val sortedFunctions = multiNode.nodes
                .map { Pair("\"${it.mangleString(false)}\"", it) }
                .sortedBy { it.first }

            println("subgraph \"cluster_${++clusterNum}\" {")
            pushIndent()

//            println("color=${multiNode.marker!!.propagationColor()};")
            println("node [style=filled];")
            println("label=\"cluster ${clusterNum}\";")

            for ((functionId, function) in sortedFunctions) {
                nodeNames[function] = functionId
                println(
                    functionId,
                    " [label=${functionId}, penwidth=2, fillcolor=${callGraph[function].marker.markerColor()}, color=${multiNode.marker!!.propagationColor()}];"
                )
            }

            popIndent()
            println("}")
        }
    }

    private fun CallGraphMarker<*>.propagationColor(): String {
        return if (this is IOCallGraphMarker) {
            return when (this.value) {
                IOCallGraphMarker.IOSafety.Any -> "black"
                IOCallGraphMarker.IOSafety.Safe -> "darkgreen"
                IOCallGraphMarker.IOSafety.Unsafe -> "darkred"
                IOCallGraphMarker.IOSafety.UnsafeTransitively -> "darkorange4"
                IOCallGraphMarker.IOSafety.Unknown -> "dodgerblue4"
            }
        } else ""
    }

    private fun CallGraphMarker<*>.markerColor(): String {
        return if (this is IOCallGraphMarker) {
            return when (this.value) {
                IOCallGraphMarker.IOSafety.Any -> "gray"
                IOCallGraphMarker.IOSafety.Safe -> "darkolivegreen1"
                IOCallGraphMarker.IOSafety.Unsafe -> "coral1"
                IOCallGraphMarker.IOSafety.UnsafeTransitively -> "darkorange"
                IOCallGraphMarker.IOSafety.Unknown -> "dodgerblue"
            }
        } else ""
    }
}