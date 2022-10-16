/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.isReal
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class CallGraphGenerationVisitor : IrElementVisitorVoid {
    private var functionNodes: MutableMap<IrFunction, FunctionDeclarationNode> = mutableMapOf()
    private var innerCalls: MutableMap<IrCall, CallGraphEdge>? = null

    val callGraph: Map<IrFunction, FunctionDeclarationNode>
        get() = functionNodes

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall) {
        val calleeFunction = expression.symbol.owner
        if (isBuiltIn(calleeFunction)) {
            return
        }

        if (calleeFunction !in functionNodes) {
            visitFunction(calleeFunction)
        }

        innerCalls?.put(expression, DirectEdge(functionNodes[calleeFunction]!!))
        super.visitCall(expression)
    }

    override fun visitFunction(declaration: IrFunction) {
        if (declaration in functionNodes || isBuiltIn(declaration)) {
            return
        }

        val decl = FunctionDeclarationNode(declaration, mutableMapOf())
        functionNodes[declaration] = decl

        val prevInnerCalls = innerCalls
        innerCalls = mutableMapOf()
        super.visitFunction(declaration)
        decl.innerCalls.putAll(innerCalls!!)
        innerCalls = prevInnerCalls
    }

    private fun isBuiltIn(declaration: IrFunction): Boolean {
        val declarationOrigin = declaration.origin
        return declarationOrigin is IrDeclarationOriginImpl && declarationOrigin.name == "BUILTIN_CLASS_METHOD"
    }
}