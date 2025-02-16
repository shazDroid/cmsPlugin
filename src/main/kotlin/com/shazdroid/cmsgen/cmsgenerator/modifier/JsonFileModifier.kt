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

    fun appendToEnglishJson(
        enFilePath: String,
        cmsKey: String,
        enContent: String,
        project: Project?,
        insertAtLine: Int = 0
    ) {
        val enFile = File(enFilePath)

        val cmsKey = cmsKey.replace(" ", "").replace("\"", "")
        val enContent = enContent.replace("\"", "")


        val enJson: MutableMap<String, String> = if (enFile.exists() && enFile.length() > 0) {
            objectMapper.readValue(enFile)
        } else {
            mutableMapOf()
        }

        // Convert the map to a list of entries to manipulate based on line
        val entries = enJson.entries.toMutableList()

        if (insertAtLine > 0 && insertAtLine <= entries.size) {
            entries.add(insertAtLine - 1, object : MutableMap.MutableEntry<String, String> {
                override val key = cmsKey
                override var value = enContent
                override fun setValue(newValue: String): String {
                    val oldValue = value
                    value = newValue
                    return oldValue
                }
            })
        } else {
            // Append at the end if insertAtLine is 0 or out of bounds
            entries.add(object : MutableMap.MutableEntry<String, String> {
                override val key = cmsKey
                override var value = enContent
                override fun setValue(newValue: String): String {
                    val oldValue = value
                    value = newValue
                    return oldValue
                }
            })
        }

        val updatedJson = entries.associate { it.toPair() }

        enFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(updatedJson))
        refreshFile(project, enFilePath)
        println("Content successfully updated in DefaultEn.json.")
    }

    fun appendToArabicJson(
        arFilePath: String,
        cmsKey: String,
        arContent: String,
        project: Project?,
        insertAtLine: Int = 0
    ) {
        val arFile = File(arFilePath)

        val cmsKey = cmsKey.replace(" ", "").replace("\"", "")
        val arContent = arContent.replace("\"", "")

        val arJson: MutableMap<String, String> = if (arFile.exists() && arFile.length() > 0) {
            objectMapper.readValue(arFile)
        } else {
            mutableMapOf()
        }

        val entries = arJson.entries.toMutableList()

        if (insertAtLine > 0 && insertAtLine <= entries.size) {
            entries.add(insertAtLine - 1, object : MutableMap.MutableEntry<String, String> {
                override val key = cmsKey
                override var value = arContent
                override fun setValue(newValue: String): String {
                    val oldValue = value
                    value = newValue
                    return oldValue
                }
            })
        } else {
            entries.add(object : MutableMap.MutableEntry<String, String> {
                override val key = cmsKey
                override var value = arContent
                override fun setValue(newValue: String): String {
                    val oldValue = value
                    value = newValue
                    return oldValue
                }
            })
        }

        val updatedJson = entries.associate { it.toPair() }

        arFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(updatedJson))
        refreshFile(project, arFilePath)
        println("Content successfully updated in DefaultAr.json.")
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
