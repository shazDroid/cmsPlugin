package com.shazdroid.cmsgen.cmsgenerator.window

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.impl.toolkit.IdeDesktopPeer.Companion.logger
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.SVGLoader
import com.shazdroid.cmsgen.cmsgenerator.cms_media_keys.AssetsDirectorySettings
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import java.awt.Image
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import javax.imageio.ImageIO
import javax.swing.*

class MediaKeyLineMarkerProvider : LineMarkerProvider {

    private val iconCache = mutableMapOf<String, Icon?>()

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        try {
            val containingFile = element.containingFile as? KtFile ?: return null
            if (containingFile.name != "CmsMediaKeys.kt") return null

            if (element !is KtProperty || !element.hasModifier(KtTokens.CONST_KEYWORD)) return null


            val companionObject = element.parentOfType<KtObjectDeclaration>()
            if (companionObject == null || !companionObject.isCompanion()) return null

            val classDeclaration = companionObject.parentOfType<KtClass>()
            if (classDeclaration?.name != "CmsMediaKeys") return null

            val initializer = element.initializer ?: return null

            val mediaBaseName = initializer.text.trim('"')

            val project = element.project
            val assetsDir = getAssetsDirectory(project)
            if (assetsDir == null) {
                println("CMS MEDIA PLUGIN ERROR -> Assets package not defined")
                return null
            }

            val imageFile = assetsDir.let { findImageFile(it, mediaBaseName) }

            val icon: Icon
            val tooltip: String
            val navigationHandler: GutterIconNavigationHandler<PsiElement>?

            if (imageFile != null) {
                icon = iconCache.getOrPut(mediaBaseName) {
                    getIconFromImage(imageFile)
                } ?: AllIcons.FileTypes.Any_type

                tooltip = "Show Image: ${imageFile.name}"

                navigationHandler = GutterIconNavigationHandler { _, _ ->
                    OpenFileDescriptor(project, imageFile).navigate(true)
                    //showImageInWindow(project, imageFile)
                }
            } else {
                icon = AllIcons.General.Error
                tooltip = "Image not found for base name: $mediaBaseName"
                navigationHandler = null
            }

            return LineMarkerInfo(
                element.nameIdentifier ?: element,
                element.textRange,
                icon,
                { tooltip },
                navigationHandler,
                GutterIconRenderer.Alignment.RIGHT
            )
        } catch (e: IOException) {
            return null
        }
    }

    fun getVirtualFileFromPath(path: String): VirtualFile? {
        val file = File(path)
        return LocalFileSystem.getInstance().findFileByIoFile(file)
    }

    private fun getAssetsDirectory(project: Project): VirtualFile? {
        val settings = AssetsDirectorySettings.getInstance(project)
        val assetsPath = settings.assetsDirectoryPath

        if (assetsPath.isEmpty()) {
            return null
        }

        val path = Paths.get(assetsPath)

        return if (path.isAbsolute) {
            LocalFileSystem.getInstance().findFileByPath(assetsPath.replace('\\', '/'))
        } else {
            project.baseDir.findFileByRelativePath(assetsPath)
        }
    }

    private fun findImageFile(assetsDir: VirtualFile, baseName: String): VirtualFile? {
        val supportedExtensions = listOf("png", "jpg", "jpeg", "gif", "bmp", "svg")
        // Prioritize extensions if needed
        for (extension in supportedExtensions) {
            val fileName = "$baseName.$extension"
            val file = findFileRecursively(assetsDir, fileName)
            if (file != null) {
                return file
            }
        }
        return null
    }

    private fun findFileRecursively(directory: VirtualFile, fileName: String): VirtualFile? {
        for (file in directory.children) {
            if (file.isDirectory) {
                val found = findFileRecursively(file, fileName)
                if (found != null) {
                    return found
                }
            } else if (file.name.equals(fileName, ignoreCase = true)) {
                return file
            }
        }
        return null
    }

    private fun getIconFromImage(imageFile: VirtualFile): Icon? {
        return try {
            when (imageFile.extension?.lowercase()) {
                "svg" -> {
                    val image = imageFile.inputStream.use { inputStream ->
                        SVGLoader.load(inputStream, 2.0f) // Scale factor 1.0
                    }
                    if (image == null) {
                        logger.error("Failed to read SVG image: ${imageFile.path}")
                        return null
                    }
                    val scaledImage = image.getScaledInstance(16, 16, Image.SCALE_SMOOTH)
                    ImageIcon(scaledImage)
                }

                else -> {
                    imageFile.inputStream.use { inputStream ->
                        val image = ImageIO.read(inputStream)
                        if (image == null) {
                            logger.error("Failed to read image: ${imageFile.path}")
                            return null
                        }
                        val scaledImage = image.getScaledInstance(16, 16, Image.SCALE_SMOOTH)
                        ImageIcon(scaledImage)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error reading image file: ${imageFile.path}", e)
            null
        }
    }


    private fun showImageInWindow(project: Project, imageFile: VirtualFile) {
        ApplicationManager.getApplication().invokeLater {
            try {
                imageFile.inputStream.use { inputStream ->
                    val image = when (imageFile.extension?.lowercase()) {
                        "svg" -> {
                            SVGLoader.load(inputStream, 1.0f) // Adjust scale if necessary
                        }

                        else -> {
                            ImageIO.read(inputStream)
                        }
                    }
                    if (image == null) {
                        Messages.showErrorDialog(project, "Failed to load image: ${imageFile.path}", "Error")
                        return@use
                    }
                    val icon = ImageIcon(image)
                    val label = JLabel(icon)
                    val scrollPane = JScrollPane(label)
                    val dialog = object : DialogWrapper(project, false) {
                        init {
                            title = imageFile.name
                            setResizable(true)
                            init()
                        }

                        override fun createCenterPanel(): JComponent {
                            return scrollPane
                        }
                    }
                    dialog.show()
                }
            } catch (e: Exception) {
                logger.error("Error displaying image: ${imageFile.path}", e)
                Messages.showErrorDialog(project, "An error occurred while displaying the image.", "Error")
            }
        }
    }
}


