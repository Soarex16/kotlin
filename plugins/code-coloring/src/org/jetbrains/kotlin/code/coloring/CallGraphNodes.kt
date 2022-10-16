/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall

sealed class CallGraphNode

sealed class CallGraphEdge

class DirectEdge(val callee: FunctionDeclarationNode) : CallGraphEdge()

class MultiEdge(val callCandidates: List<FunctionDeclarationNode>) : CallGraphEdge()

class FunctionDeclarationNode(val ir: IrFunction, val innerCalls: MutableMap<IrCall, CallGraphEdge>) : CallGraphNode()