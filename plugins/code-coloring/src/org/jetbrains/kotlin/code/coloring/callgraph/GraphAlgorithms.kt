/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.callgraph

import org.jetbrains.kotlin.code.coloring.demo.IOCallGraphMarker

/**
 * Copy from [org.jetbrains.kotlin.backend.konan.GraphAlgorithms]
 */
interface DirectedGraphNode<out K> {
    val key: K
    val directEdges: List<K>?
    val reversedEdges: List<K>?
}

interface DirectedGraph<K, out N : DirectedGraphNode<K>> {
    val nodes: Collection<N>
    operator fun get(key: K): N
    operator fun contains(key: K): Boolean
}

class DirectedGraphMultiNode<out K>(val nodes: Set<K>, var marker: CallGraphMarker<*>? = null)

class DirectedGraphCondensation<out K>(val topologicalOrder: List<DirectedGraphMultiNode<K>>)

// The Kosoraju-Sharir algorithm.
class DirectedGraphCondensationBuilder<K, out N : DirectedGraphNode<K>>(private val graph: DirectedGraph<K, N>) {
    private val visited = mutableSetOf<K>()
    private val order = mutableListOf<N>()
    private val nodeToMultiNodeMap = mutableMapOf<N, DirectedGraphMultiNode<K>>()
    private val multiNodesOrder = mutableListOf<DirectedGraphMultiNode<K>>()

    fun build(): DirectedGraphCondensation<K> {
        // First phase.
        graph.nodes.forEach {
            if (!visited.contains(it.key))
                findOrder(it)
        }

        // Second phase.
        visited.clear()
        val multiNodes = mutableListOf<DirectedGraphMultiNode<K>>()
        order.reversed().forEach {
            if (!visited.contains(it.key)) {
                val nodes = mutableSetOf<K>()
                paint(it, nodes)
                multiNodes += DirectedGraphMultiNode(nodes)
            }
        }

        // Topsort of built condensation.
        multiNodes.forEach { multiNode ->
            multiNode.nodes.forEach { nodeToMultiNodeMap[graph.get(it)] = multiNode }
        }
        visited.clear()
        multiNodes.forEach {
            if (!visited.contains(it.nodes.first()))
                findMultiNodesOrder(it)
        }

        return DirectedGraphCondensation(multiNodesOrder.reversed())
    }

    private fun findOrder(node: N) {
        visited += node.key
        node.directEdges?.forEach {
            if (!visited.contains(it))
                findOrder(graph.get(it))
        }
        order += node
    }

    private fun paint(node: N, multiNode: MutableSet<K>) {
        visited += node.key
        multiNode += node.key
        node.reversedEdges?.forEach {
            if (!visited.contains(it))
                paint(graph.get(it), multiNode)
        }
    }

    private fun findMultiNodesOrder(node: DirectedGraphMultiNode<K>) {
        visited.addAll(node.nodes)
        node.nodes.forEach {
            graph.get(it).directEdges?.forEach { key ->
                if (!visited.contains(key))
                    findMultiNodesOrder(nodeToMultiNodeMap[graph.get(key)]!!)
            }
        }
        multiNodesOrder += node
    }
}