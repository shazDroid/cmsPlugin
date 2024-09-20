package com.shazdroid.cmsgen.cmsgenerator.modifier

import com.intellij.openapi.ui.Messages
import java.io.File

class FileModifier {
    // Function to convert camel case to snake case
    private fun camelToSnakeCase(input: String): String {
        return input.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
    }

    fun appendCmsKeyToFile(filePath: String, cmsKey: String) : Boolean {
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

        println("CmsKey '$cmsKey' has been successfully added to the file with proper formatting.")

        return true
    }
}
