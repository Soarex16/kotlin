/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractDiagnosticTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractBytecodeListingTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBytecodeListingTest
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

// ---------------------------- codegen ----------------------------

open class AbstractBlackBoxCodegenTestForNoArg : AbstractBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableNoArg()
    }
}

open class AbstractIrBlackBoxCodegenTestForNoArg : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableNoArg()
    }
}

// ---------------------------- bytecode ----------------------------

open class AbstractBytecodeListingTestForNoArg : AbstractBytecodeListingTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableNoArg()
    }
}

open class AbstractIrBytecodeListingTestForNoArg : AbstractIrBytecodeListingTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableNoArg()
    }
}

// ---------------------------- diagnostic ----------------------------

abstract class AbstractDiagnosticsTestForNoArg : AbstractDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableNoArg()
    }
}

// ---------------------------- configurator ----------------------------

private fun TestConfigurationBuilder.enableNoArg() {
    useConfigurators(::NoArgEnvironmentConfigurator)
}

class NoArgEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        private val NOARG_ANNOTATIONS = listOf("NoArg", "NoArg2", "test.NoArg")
    }

    override val directiveContainers: List<DirectivesContainer> = listOf(NoArgDirectives)

    override fun registerCompilerExtensions(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        NoArgComponentRegistrar.registerNoArgComponents(
            project,
            NOARG_ANNOTATIONS,
            useIr = module.targetBackend?.isIR == true,
            invokeInitializers = NoArgDirectives.INVOKE_INITIALIZERS in module.directives
        )
    }
}

object NoArgDirectives : SimpleDirectivesContainer() {
    val INVOKE_INITIALIZERS by directive("Enable 'Invoke initializers' mode")
}
