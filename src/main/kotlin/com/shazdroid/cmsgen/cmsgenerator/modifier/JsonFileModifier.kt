package com.shazdroid.cmsgen.cmsgenerator.modifier


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File

class JsonFileModifier {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule())

    fun appendToEnglishJson(enFilePath: String, cmsKey: String, enContent: String, project: Project?) {
        val enFile = File(enFilePath)

        val cmsKey = cmsKey.replace(" ","").replace("\"","")
        val enContent = enContent.replace("\"","")

        // Read the existing content
        val enJson: MutableMap<String, String> = if (enFile.exists() && enFile.length() > 0) {
            objectMapper.readValue(enFile)
        } else {
            mutableMapOf()
        }

        // Append the new key-value pair
        enJson[cmsKey] = enContent

        // Write the updated content back while preserving formatting
        enFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(enJson))
        refreshFile(project, enFilePath)
        println("Content successfully appended to DefaultEn.json.")
    }

    fun appendToArabicJson(arFilePath: String, cmsKey: String, arContent: String, project: Project?) {
        val arFile = File(arFilePath)

        val cmsKey = cmsKey.replace(" ","")
        val arContent = arContent.replace("\"","")

        // Read the existing content
        val arJson: MutableMap<String, String> = if (arFile.exists() && arFile.length() > 0) {
            objectMapper.readValue(arFile)
        } else {
            mutableMapOf()
        }

        // Append the new key-value pair
        arJson[cmsKey] = arContent

        // Write the updated content back while preserving formatting
        arFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arJson))
        refreshFile(project, arFilePath)
        println("Content successfully appended to DefaultAr.json.")
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
