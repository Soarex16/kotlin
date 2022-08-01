/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.workers

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import kotlin.reflect.KClass

fun transformWorkerFileToModule(module: IrModuleFragment, file: IrFile): IrModuleFragment {
    // Понять как можно сформировать 2 точки входа
    return IrModuleFragmentImpl(
        ModuleDescriptorImpl(
            Name.special("<${file.fqName}.worker>"),
            LockBasedStorageManager.NO_LOCKS,
            DefaultBuiltIns()
        ),
        module.irBuiltins,
        listOf(file)
    )
}