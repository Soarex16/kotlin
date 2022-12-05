/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.runners

import org.jetbrains.kotlin.code.coloring.callgraph.CallGraphBuilder
import org.jetbrains.kotlin.code.coloring.callgraph.propagateMarkers
import org.jetbrains.kotlin.test.backend.handlers.AbstractIrHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

class CallGraphDumpHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CallGraphDirectives)

    private var alreadyDumped = false

    private val builder = StringBuilder()

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (alreadyDumped || CallGraphDirectives.DUMP_CALL_GRAPH !in module.directives) return
        val (callGraph, condensation) = CallGraphBuilder.processModuleFragment(info.irModuleFragment)
        propagateMarkers(callGraph, condensation)
        CallGraphRenderer(callGraph, condensation, builder).renderDotGraph()
        alreadyDumped = true
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!alreadyDumped) return
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.firstOrNull()!!
        val expectedFile = testDataFile.parentFile.resolve("${testDataFile.nameWithoutExtension}.dot")
        assertions.assertEqualsToFile(expectedFile, builder.toString())
    }
}