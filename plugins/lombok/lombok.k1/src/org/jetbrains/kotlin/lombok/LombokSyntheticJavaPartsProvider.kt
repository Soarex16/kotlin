/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.lombok.config.LombokConfig
import org.jetbrains.kotlin.lombok.processor.*
import org.jetbrains.kotlin.lombok.utils.getJavaClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.SyntheticJavaPartsProvider
import java.util.*

/**
 * Provides synthetic parts to java classes (from current compilation unit), which will be generated by lombok AnnotationProcessor
 * So kotlin can reference lombok members.
 */
@Suppress("IncorrectFormatting") // KTIJ-22227
class LombokSyntheticJavaPartsProvider(config: LombokConfig) : SyntheticJavaPartsProvider {

    private val processors = listOf(
        GetterProcessor(config),
        SetterProcessor(config),
        WithProcessor(),
        NoArgsConstructorProcessor(),
        AllArgsConstructorProcessor(),
        RequiredArgsConstructorProcessor()
    )

    /**
     * kotlin resolve references in two calls - first it gets names, then actual member descriptor
     * but for us it is much easier to run full generation for class once
     * hence we cache results and reuse it
     */
    private val partsCache: MutableMap<ClassDescriptor, SyntheticParts> = HashMap()

    context(LazyJavaResolverContext)
    override fun getMethodNames(thisDescriptor: ClassDescriptor): List<Name> =
        getSyntheticParts(thisDescriptor).methods.map { it.name }

    context(LazyJavaResolverContext)
    override fun generateMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        val methods = getSyntheticParts(thisDescriptor).methods.filter { it.name == name }
        addNonExistent(result, methods)
    }

    context(LazyJavaResolverContext)
    override fun getStaticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> =
        getSyntheticParts(thisDescriptor).staticFunctions.map { it.name }

    context(LazyJavaResolverContext)
    override fun generateStaticFunctions(thisDescriptor: ClassDescriptor, name: Name, result: MutableCollection<SimpleFunctionDescriptor>) {
        val functions = getSyntheticParts(thisDescriptor).staticFunctions.filter { it.name == name }
        addNonExistent(result, functions)
    }

    context(LazyJavaResolverContext)
    override fun generateConstructors(thisDescriptor: ClassDescriptor, result: MutableList<ClassConstructorDescriptor>) {
        val constructors = getSyntheticParts(thisDescriptor).constructors
        addNonExistent(result, constructors)
    }

    context(LazyJavaResolverContext)
    override fun getNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> {
        return getSyntheticParts(thisDescriptor).classes.map { it.name }
    }

    context(LazyJavaResolverContext)
    override fun generateNestedClass(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableList<ClassDescriptor>
    ) {
        result += getSyntheticParts(thisDescriptor).classes.filter { it.name == name }
    }

    context(LazyJavaResolverContext)
    private fun getSyntheticParts(descriptor: ClassDescriptor): SyntheticParts =
        descriptor.getJavaClass()?.let {
            partsCache.getOrPut(descriptor) {
                computeSyntheticParts(descriptor)
            }
        } ?: SyntheticParts.Empty

    context(LazyJavaResolverContext)
    private fun computeSyntheticParts(descriptor: ClassDescriptor): SyntheticParts {
        val builder = SyntheticPartsBuilder()
        processors.forEach { it.contribute(descriptor, builder) }
        return builder.build()
    }

    /**
     * Deduplicates generated functions using name and argument counts, as lombok does
     */
    private fun <T : FunctionDescriptor> addNonExistent(result: MutableCollection<T>, toAdd: List<T>) {
        toAdd.forEach { f ->
            if (result.none { sameSignature(it, f) }) {
                result += f
            }
        }
    }


    companion object {

        /**
         * Lombok treat functions as having the same signature by arguments count only
         * Corresponding code in lombok - https://github.com/projectlombok/lombok/blob/v1.18.20/src/core/lombok/javac/handlers/JavacHandlerUtil.java#L752
         */
        private fun sameSignature(a: FunctionDescriptor, b: FunctionDescriptor): Boolean {
            val aVararg = a.valueParameters.any { it.varargElementType != null }
            val bVararg = b.valueParameters.any { it.varargElementType != null }
            return aVararg && bVararg ||
                    aVararg && b.valueParameters.size >= (a.valueParameters.size - 1) ||
                    bVararg && a.valueParameters.size >= (b.valueParameters.size - 1) ||
                    a.valueParameters.size == b.valueParameters.size
        }
    }
}
