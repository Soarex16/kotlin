/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.runners

import org.jetbrains.kotlin.code.coloring.DirectEdge
import org.jetbrains.kotlin.code.coloring.FunctionDeclarationNode
import org.jetbrains.kotlin.code.coloring.MultiEdge
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr.mangleString
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr.signatureString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer

class CallGraphRenderer(private val callGraph: Map<IrFunction, FunctionDeclarationNode>, builder: StringBuilder) {
    companion object {
        private const val EDGE = " -> "
        private const val RED = "red"
        private const val BLUE = "blue"
    }

    private val printer = Printer(builder)

    private val nodeNames = mutableMapOf<IrFunction, String>()

    fun renderDotGraph() {
        val functionByPackage = groupFunctionsByPackage(callGraph.keys)
        printer
            .println("digraph call_graph {")
            .pushIndent()
            .println()

        printer.renderNodes(functionByPackage)
        printer.renderEdges(callGraph.values)

        printer
            .popIndent()
            .println("}")
    }

    private fun groupFunctionsByPackage(callGraph: Set<IrFunction>): Map<FqName, List<IrFunction>> {
        return callGraph.groupBy { it.getPackageFragment().fqName }
    }

    private fun Printer.renderEdges(functions: Iterable<FunctionDeclarationNode>) {
        val edges = mutableListOf<String>()
        for (decl in functions) {
            for ((_, edge) in decl.innerCalls) {
                when (edge) {
                    is DirectEdge -> edges.add("${nodeNames[decl.ir]}${EDGE}${nodeNames[edge.callee.ir]};")

                    is MultiEdge -> {
                        for (target in edge.callCandidates) {
                            edges.add("${nodeNames[decl.ir]}${EDGE}${nodeNames[target.ir]} [style=dashed];")
                        }
                    }
                }
            }
        }

        for (edge in edges.sorted()) {
            println(edge)
        }
    }

    private fun Printer.renderNodes(functionByPackage: Map<FqName, List<IrFunction>>) {
        val sortedPackages = functionByPackage.entries.sortedBy { it.key.asString() }
        for ((packageFqn, functions) in sortedPackages) {
            println("subgraph \"cluster_${packageFqn}\" {")
            pushIndent()

            println("node [style=filled];")
            println("label=\"package ${packageFqn}\";")
            println("color=$RED;")

            val sortedFunctions = functions
                .map { Pair("\"${it.mangleString(false)}\"", it) }
                .sortedBy { it.first }
            for ((functionId, function) in sortedFunctions) {
                nodeNames[function] = functionId
                println(functionId, " [label=\"${function.signatureString(false)}\"];")
            }

            popIndent()
            println("}")
        }
    }
}