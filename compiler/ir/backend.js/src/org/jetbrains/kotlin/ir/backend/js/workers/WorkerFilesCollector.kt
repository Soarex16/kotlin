/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.workers

import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.FqName

private val WEB_WORKER_ANNOTATION = FqName("kotlinx.webworkers.WebWorker")

class WorkerFilesCollector : IrElementTransformer<MutableList<IrFile>> {
    override fun visitModuleFragment(declaration: IrModuleFragment, data: MutableList<IrFile>): IrModuleFragment {
        if (declaration.files.size < 2) return declaration

        val (workerFiles, restFiles) = declaration.files.partition { it.hasAnnotation(WEB_WORKER_ANNOTATION) }
        data.addAll(workerFiles)

        return IrModuleFragmentImpl(
            declaration.descriptor,
            declaration.irBuiltins,
            restFiles
        )
    }
}