/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.runners

import org.jetbrains.kotlin.code.coloring.services.ExtensionRegistrarConfigurator
import org.jetbrains.kotlin.code.coloring.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

fun TestConfigurationBuilder.commonFirWithPluginFrontendConfiguration() {
    baseFirDiagnosticTestConfiguration()

    globalDefaults {
        targetPlatform = JsPlatforms.defaultJsPlatform
        targetBackend = TargetBackend.JS_IR
    }

    defaultDirectives {
        +ENABLE_PLUGIN_PHASES
        +FIR_DUMP
    }

    useCustomRuntimeClasspathProviders(::RuntimeClasspathProvider)

    useConfigurators(
        ::ExtensionRegistrarConfigurator,
        ::JsEnvironmentConfigurator,
    )
}
