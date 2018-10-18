/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import org.gradle.api.*
import org.gradle.api.provider.Provider
<<<<<<< HEAD
import org.jetbrains.kotlin.gradle.getMethodOrNull
=======
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModelBuilder.Companion.getTargets
>>>>>>> 9669d7d... Build artifacts via compilations, not via tasks
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.utils.addToStdlib.flatMapToNullable
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import java.io.File

class KonanModelBuilder : ModelBuilderService {
    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder
            .create(project, e, "Gradle import errors")
            .withDescription("Unable to build Konan project configuration")
    }

    override fun canBuild(modelName: String?): Boolean {
        return modelName == KonanModel::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any? {
        println("build")

        val targets = project.getTargets() ?: error("Targets not found")

        val artifacts = targets
            .flatMap { it["getCompilations"] as NamedDomainObjectContainer<*> }
            .flatMap { compilation ->
                val outputKinds = compilation["getOutputKinds"] as? List<*>
                val buildTypes = compilation["getBuildTypes"] as? List<*>
                outputKinds.orEmpty().flatMap { outputKind ->
                    buildTypes.orEmpty().mapNotNull { buildType ->
                        outputKind ?: return@mapNotNull null
                        buildType ?: return@mapNotNull null
                        compilation["getLinkTask", outputKind, buildType] as? Task
                    }
                }
            }
            .mapNotNull { buildArtifact(it) }

        return KonanModelImpl(artifacts)
    }

    private fun buildArtifact(task: Task): KonanModelArtifact? {
        val outputKind = task["getOutputKind"]["name"] as? String ?: return null
        val konanTargetName = task["getTarget"] as? String ?: error("No arch target found")
        val outputFile = (task["getOutputFile"] as? Provider<*>)?.orNull as? File ?: return null
        val compilationTarget = task["getCompilation"]["getTarget"]
        val compilationTargetName = compilationTarget["getName"] as? String ?: return null
        val isTests = task["getProcessTests"] as? Boolean ?: return null

        return KonanModelArtifactImpl(
            compilationTargetName,
            CompilerOutputKind.valueOf(outputKind),
            konanTargetName,
            outputFile,
            task.name,
            isTests
        ).also {
            println(it)
        }
    }

    private operator fun Any?.get(methodName: String, vararg params: Any): Any? {
        return this[methodName, params.map { it.javaClass }, params.toList()]
    }

    private operator fun Any?.get(methodName: String, paramTypes: List<Class<*>>, params: List<Any?>): Any? {
        if (this == null) return null
        return this::class.java.getMethodOrNull(methodName, *paramTypes.toTypedArray())?.invoke(this, *params.toTypedArray())
    }

}