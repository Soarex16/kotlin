/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.runners

import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object CallGraphDirectives : SimpleDirectivesContainer() {
    val DUMP_CALL_GRAPH by directive(
        description = """
            Dumps call graph to `testName.dot` file
            This directive may be applied only to all modules
        """.trimIndent(),
        applicability = DirectiveApplicability.Global
    )
}
