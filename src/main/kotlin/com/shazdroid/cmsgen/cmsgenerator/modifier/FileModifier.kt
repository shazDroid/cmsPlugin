package com.shazdroid.cmsgen.cmsgenerator.modifier

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File

class FileModifier {
    // Function to convert camel case to snake case
    fun camelToSnakeCase(input: String): String {
        // Trim leading and trailing spaces and remove extra spaces between words
        val cleanedInput = input.trim().replace(Regex("\\s+"), " ")

        // If the cleaned input contains spaces, treat it as multiple words
        return if (cleanedInput.contains(" ")) {
            // Convert multiple words: remove spaces, convert to snake case, and uppercase
            cleanedInput
                .replace(" ", "")                           // Remove all spaces
                .replace(Regex("([a-z])([A-Z])"), "$1_$2")  // Convert camel case to snake case
                .uppercase()                                // Convert to uppercase
        } else {
            // For single word: just convert to uppercase
            cleanedInput
                .replace(Regex("([a-z])([A-Z])"), "$1_$2")  // Convert camel case to snake case
                .uppercase()
        }
    }

    fun appendCmsKeyToFile(filePath: String, cmsKey: String, project: Project?) : Boolean {
        // Read the file content
        val file = File(filePath)
        if (!file.exists()) {
            Messages.showMessageDialog(
                "File does not exist.",
                "File not found",
                Messages.getErrorIcon()
            )
            println("File does not exist.")
            return false
        }

        var content = file.readText()

        // Convert the camelCase cmsKey to SNAKE_CASE
        val keyUpperCase = camelToSnakeCase(cmsKey)

        // Remove any spaces from actual keys
        val cmsKey = cmsKey.replace(" ", "")

        // Check if the companion object already contains the new constant to avoid duplicates
        if (content.contains("const val $keyUpperCase")) {
            Messages.showMessageDialog(
                "Key '$keyUpperCase' already exists in the file.",
                "Duplicate key '$keyUpperCase'",
                Messages.getErrorIcon()
            )
            println("Key '$keyUpperCase' already exists in the file.")
            return false
        }

        // Regex to find the companion object block without modifying the rest of the file
        val companionObjectPattern = Regex("(companion\\s+object\\s*\\{)([\\s\\S]*?)(\\})")

        val matchResult = companionObjectPattern.find(content)
        if (matchResult == null) {
            Messages.showMessageDialog(
                "Companion object not found in CmsKeyMapper.kt",
                "File Read'$keyUpperCase'",
                Messages.getErrorIcon()
            )
            println("Companion object not found.")
            return false
        }

        val beforeCompanion = content.substring(0, matchResult.range.first) // Everything before the companion object
        val insideCompanion = matchResult.groups[2]?.value?.trimEnd() ?: ""  // Inside the companion object
        val afterCompanion = content.substring(matchResult.range.last + 1)   // Everything after the companion object

        // Prepare the new constant string with proper formatting
        val newConstant = "\n        const val $keyUpperCase = \"$cmsKey\"\n"

        // Construct the new content
        val updatedContent = beforeCompanion +
                matchResult.groups[1]?.value +  // companion object start
                insideCompanion + newConstant + "\n    }" +
                afterCompanion

        // Write the modified content back to the file
        file.writeText(updatedContent)

        refreshFile(project, filePath)

        println("CmsKey '$cmsKey' has been successfully added to the file with proper formatting.")

        return true
    }

    private fun refreshFile(project: Project?, filePath: String) {
        if (project == null) return

        val virtualFile: VirtualFile? = VirtualFileManager.getInstance().findFileByUrl("file://$filePath")
        if (virtualFile != null) {
            // Refresh the file in the VFS
            virtualFile.refresh(false, true)

            // Optionally notify the editor to refresh
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
    }
}
