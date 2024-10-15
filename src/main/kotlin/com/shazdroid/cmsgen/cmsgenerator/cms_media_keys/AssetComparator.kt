package com.shazdroid.cmsgen.cmsgenerator.cms_media_keys


import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty

data class AssetInfo(val name: String, val path: String)
data class KeyInfo(val name: String, val location: String)

data class ComparisonResult(
    val missingAssets: List<AssetInfo>,
    val unusedKeys: List<KeyInfo>
)

object AssetsComparator {
    fun compareAssetsWithKeys(project: Project): ComparisonResult {
        val assetsDir = getAssetsDirectory(project) ?: return ComparisonResult(emptyList(), emptyList())
        val assetsSet = mutableSetOf<String>()
        val assetsInfo = mutableListOf<AssetInfo>()
        collectAssetNames(assetsDir, assetsSet, assetsInfo)

        val cmsMediaKeysFile = findCmsMediaKeysFile(project) ?: return ComparisonResult(
            assetsInfo.map { AssetInfo(it.name, it.path) },
            emptyList()
        )
        val definedKeysSet = mutableSetOf<String>()
        val definedKeysInfo = mutableListOf<KeyInfo>()
        collectDefinedKeys(cmsMediaKeysFile, definedKeysSet, definedKeysInfo)

        val missingAssets = assetsInfo.filter { !definedKeysSet.contains(it.name) }
        val unusedKeys = definedKeysInfo.filter { !assetsSet.contains(it.name) }

        return ComparisonResult(missingAssets, unusedKeys)
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

    private fun collectAssetNames(
        directory: VirtualFile,
        assetsSet: MutableSet<String>,
        assetsInfo: MutableList<AssetInfo>
    ) {
        for (file in directory.children) {
            if (file.isDirectory) {
                collectAssetNames(file, assetsSet, assetsInfo)
            } else {
                val nameWithoutExtension = file.nameWithoutExtension
                assetsSet.add(nameWithoutExtension)
                assetsInfo.add(AssetInfo(nameWithoutExtension, file.path))
            }
        }
    }

    fun findCmsMediaKeysFile(project: Project): KtFile? {
        val psiManager = PsiManager.getInstance(project)
        val virtualFiles = VfsUtil.collectChildrenRecursively(project.baseDir)
        for (vf in virtualFiles) {
            if (vf.isDirectory || vf.name != "CmsMediaKeys.kt") continue
            val psiFile = psiManager.findFile(vf) as? KtFile
            if (psiFile != null) {
                return psiFile
            }
        }
        return null
    }

    private fun collectDefinedKeys(
        cmsMediaKeysFile: KtFile,
        definedKeysSet: MutableSet<String>,
        definedKeysInfo: MutableList<KeyInfo>
    ) {
        val declarations = cmsMediaKeysFile.declarations
        for (declaration in declarations) {
            if (declaration is KtClass && declaration.name == "CmsMediaKeys") {
                val companionObject = declaration.companionObjects.firstOrNull()
                if (companionObject != null) {
                    for (member in companionObject.declarations) {
                        if (member is KtProperty && member.hasModifier(KtTokens.CONST_KEYWORD)) {
                            val initializer = member.initializer
                            if (initializer != null && initializer !is PsiErrorElement) {
                                val keyName = initializer.text.trim('"')
                                val keyInfo = KeyInfo(keyName, cmsMediaKeysFile.virtualFile.path)
                                definedKeysSet.add(keyName)
                                definedKeysInfo.add(keyInfo)
                            }
                        }
                    }
                }
            }
        }
    }
}
