package com.shazdroid.cmsgen.cmsgenerator.cms_media_keys


import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty

class CompareAssetsAction : AnAction("Compare Assets with CmsMediaKeys") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project).getToolWindow("Assets Comparison")
        toolWindow?.show()
    }

    private fun getAssetsDirectory(project: Project): VirtualFile? {
        val settings = AssetsDirectorySettings.getInstance(project)
        var assetsPath = settings.assetsDirectoryPath
        if (assetsPath.isEmpty()) return null
        assetsPath = assetsPath.trim().replace('\\', '/')
        val path = java.nio.file.Paths.get(assetsPath)
        return if (path.isAbsolute) {
            com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(assetsPath)
        } else {
            project.baseDir.findFileByRelativePath(assetsPath)
        }
    }

    private fun collectAssetNames(directory: VirtualFile, assetsSet: MutableSet<String>) {
        for (file in directory.children) {
            if (file.isDirectory) {
                collectAssetNames(file, assetsSet)
            } else {
                val nameWithoutExtension = file.nameWithoutExtension
                assetsSet.add(nameWithoutExtension)
            }
        }
    }

    private fun findCmsMediaKeysFile(project: Project): KtFile? {
        val psiManager = com.intellij.psi.PsiManager.getInstance(project)
        val virtualFiles = com.intellij.openapi.vfs.VfsUtil.collectChildrenRecursively(project.baseDir)
        for (vf in virtualFiles) {
            if (vf.isDirectory || vf.name != "CmsMediaKeys.kt") continue
            val psiFile = psiManager.findFile(vf) as? KtFile
            if (psiFile != null) {
                return psiFile
            }
        }
        return null
    }

    private fun collectDefinedKeys(cmsMediaKeysFile: KtFile): Set<String> {
        val definedKeys = mutableSetOf<String>()
        val declarations = cmsMediaKeysFile.declarations
        for (declaration in declarations) {
            if (declaration is KtClass && declaration.name == "CmsMediaKeys") {
                val companionObject = declaration.companionObjects.firstOrNull()
                if (companionObject != null) {
                    for (member in companionObject.declarations) {
                        if (member is KtProperty && member.hasModifier(org.jetbrains.kotlin.lexer.KtTokens.CONST_KEYWORD)) {
                            val initializer = member.initializer
                            if (initializer != null && initializer !is com.intellij.psi.PsiErrorElement) {
                                val keyName = initializer.text.trim('"')
                                definedKeys.add(keyName)
                            }
                        }
                    }
                }
            }
        }
        return definedKeys
    }
}
