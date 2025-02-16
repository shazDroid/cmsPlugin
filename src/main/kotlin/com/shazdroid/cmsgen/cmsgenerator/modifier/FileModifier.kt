package com.shazdroid.cmsgen.cmsgenerator.modifier

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File
import java.io.IOException

class FileModifier {
    fun camelToSnakeCase(input: String): String {
        val cleanedInput = input.trim().replace(Regex("\\s+"), " ")

        return if (cleanedInput.contains(" ")) {
            cleanedInput
                .replace(" ", "")
                .replace(Regex("([a-z])([A-Z])"), "$1_$2")
                .uppercase()
        } else {
            cleanedInput
                .replace(Regex("([a-z])([A-Z])"), "$1_$2")
                .uppercase()
        }
    }

    enum class FileOperationResult {
        SUCCESS,
        FILE_NOT_FOUND,
        DUPLICATE_KEY,
        COMPANION_OBJECT_NOT_FOUND,
        WRITE_ERROR
    }

    fun appendCmsKeyToFile(filePath: String, cmsKey: String, project: Project?): FileOperationResult {
        val file = File(filePath)
        if (!file.exists()) {
            showDialog("File does not exist.", "File not found")
            println("File does not exist.")
            return FileOperationResult.FILE_NOT_FOUND
        }

        var content = file.readText(Charsets.UTF_8)
        val keyUpperCase = camelToSnakeCase(cmsKey)
        val cmsKey = cmsKey.replace(" ", "")

        if (content.contains("const val $keyUpperCase")) {
            showDialog("Key '$keyUpperCase' already exists in the file.", "Duplicate key '$keyUpperCase'")
            println("Key '$keyUpperCase' already exists in the file.")
            return FileOperationResult.DUPLICATE_KEY
        }

        val companionObjectPattern = Regex("(companion\\s+object\\s*\\{)(.*?)(\\})", RegexOption.DOT_MATCHES_ALL)
        val matchResult = companionObjectPattern.find(content)

        if (matchResult == null) {
            showDialog("Companion object not found in CmsKeyMapper.kt", "File Read '$keyUpperCase'")
            println("Companion object not found.")
            return FileOperationResult.COMPANION_OBJECT_NOT_FOUND
        }

        val beforeCompanion = content.substring(0, matchResult.range.first)
        val insideCompanion = matchResult.groups[2]?.value?.trimEnd() ?: ""
        val afterCompanion = content.substring(matchResult.range.last + 1)

        val newConstant = "\n        const val $keyUpperCase = \"$cmsKey\"\n"
        val updatedContent = beforeCompanion +
                matchResult.groups[1]?.value +
                insideCompanion + newConstant + "\n    }" +
                afterCompanion

        return try {
            file.writeText(updatedContent, Charsets.UTF_8)
            refreshFile(project, filePath)
            println("CmsKey '$cmsKey' has been successfully added to the file with proper formatting.")
            FileOperationResult.SUCCESS
        } catch (e: IOException) {
            e.printStackTrace()
            FileOperationResult.WRITE_ERROR
        }
    }

    private fun showDialog(message: String, title: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showMessageDialog(message, title, Messages.getErrorIcon())
        }
    }

    private fun refreshFile(project: Project?, filePath: String) {
        if (project == null) return

        val virtualFile: VirtualFile? = VirtualFileManager.getInstance().findFileByUrl("file://$filePath")
        if (virtualFile != null) {
            virtualFile.refresh(false, true)
           // FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
    }
}
