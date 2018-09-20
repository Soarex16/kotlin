/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.configure

import com.android.tools.idea.gradle.project.model.AndroidModuleModel
import com.android.tools.idea.gradle.util.ContentEntries.findParentContentEntry
import com.android.tools.idea.io.FilePaths
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.service.project.manage.ContentRootDataService.CREATE_EMPTY_DIRECTORIES
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.containers.stream
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.gradle.KotlinCompilation
import org.jetbrains.kotlin.gradle.KotlinPlatform
import org.jetbrains.kotlin.idea.configuration.GradleProjectImportHandler
import org.jetbrains.kotlin.idea.configuration.KotlinSourceSetDataService
import org.jetbrains.kotlin.idea.configuration.kotlinAndroidSourceSets
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import java.io.File
import java.io.IOException

class KotlinAndroidGradleMPPModuleDataService : AbstractProjectDataService<ModuleData, Void>() {
    override fun getTargetDataKey() = ProjectKeys.MODULE

    private fun shouldCreateEmptySourceRoots(
        moduleDataNode: DataNode<ModuleData>,
        module: Module
    ): Boolean {
        val projectDataNode = ExternalSystemApiUtil.findParent(moduleDataNode, ProjectKeys.PROJECT) ?: return false
        if (projectDataNode.getUserData<Boolean>(CREATE_EMPTY_DIRECTORIES) == true) return true

        val projectSystemId = projectDataNode.data.owner
        val externalSystemSettings = ExternalSystemApiUtil.getSettings(module.project, projectSystemId)

        val path = ExternalSystemModulePropertyManager.getInstance(module).getRootProjectPath() ?: return false
        return externalSystemSettings.getLinkedProjectSettings(path)?.isCreateEmptyContentRootDirectories ?: false
    }

    override fun postProcess(
        toImport: MutableCollection<DataNode<ModuleData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        for (nodeToImport in toImport) {
            val projectNode = ExternalSystemApiUtil.findParent(nodeToImport, ProjectKeys.PROJECT) ?: continue
            val moduleData = nodeToImport.data
            val module = modelsProvider.findIdeModule(moduleData) ?: continue
            val shouldCreateEmptySourceRoots = shouldCreateEmptySourceRoots(nodeToImport, module)
            val androidModel = AndroidModuleModel.get(module) ?: continue
            val variantName = androidModel.selectedVariant.name
            val sourceSetInfo = nodeToImport.kotlinAndroidSourceSets?.firstOrNull { it.kotlinModule.name == variantName } ?: continue
            val compilation = sourceSetInfo.kotlinModule as? KotlinCompilation ?: continue
            val rootModel = modelsProvider.getModifiableRootModel(module)
            for (sourceSet in compilation.sourceSets) {
                if (sourceSet.platform == KotlinPlatform.ANDROID) {
                    val sourceType = if (sourceSet.isTestModule) JavaSourceRootType.TEST_SOURCE else JavaSourceRootType.SOURCE
                    val resourceType = if (sourceSet.isTestModule) JavaResourceRootType.TEST_RESOURCE else JavaResourceRootType.RESOURCE
                    sourceSet.sourceDirs.forEach { addSourceRoot(it, sourceType, rootModel, shouldCreateEmptySourceRoots) }
                    sourceSet.resourceDirs.forEach { addSourceRoot(it, resourceType, rootModel, shouldCreateEmptySourceRoots) }
                } else {
                    val sourceSetId = sourceSetInfo.sourceSetIdsByName[sourceSet.name] ?: continue
                    val sourceSetData = ExternalSystemApiUtil.findFirstRecursively(projectNode) {
                        (it.data as? ModuleData)?.id == sourceSetId
                    }?.data as? ModuleData ?: continue
                    val sourceSetModule = modelsProvider.findIdeModule(sourceSetData) ?: continue
                    rootModel.addModuleOrderEntry(sourceSetModule)
                }
            }

            KotlinSourceSetDataService.configureFacet(moduleData, sourceSetInfo, nodeToImport, module, modelsProvider)

            val kotlinFacet = KotlinFacet.get(module)
            if (kotlinFacet != null) {
                GradleProjectImportHandler.getInstances(project).forEach { it.importByModule(kotlinFacet, nodeToImport) }
            }
        }
    }

    private fun addSourceRoot(
        sourceRoot: File,
        type: JpsModuleSourceRootType<*>,
        rootModel: ModifiableRootModel,
        shouldCreateEmptySourceRoots: Boolean
    ) {
        val parent = findParentContentEntry(sourceRoot, rootModel.contentEntries.stream()) ?: return
        val url = FilePaths.pathToIdeaUrl(sourceRoot)
        parent.addSourceFolder(url, type)
        if (shouldCreateEmptySourceRoots) {
            ExternalSystemApiUtil.doWriteAction {
                try {
                    VfsUtil.createDirectoryIfMissing(sourceRoot.path)
                } catch (e: IOException) {
                    LOG.warn(String.format("Unable to create directory for the path: %s", sourceRoot.path), e)
                }
            }
        }
    }

    companion object {
        private val LOG = Logger.getInstance(KotlinAndroidGradleMPPModuleDataService::class.java)
    }
}