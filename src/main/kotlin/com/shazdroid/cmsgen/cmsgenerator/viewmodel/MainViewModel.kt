package com.shazdroid.cmsgen.cmsgenerator.viewmodel

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.openapi.application.CachedSingletonsRegistry
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.shazdroid.cmsgen.cmsgenerator.modifier.FileModifier
import com.shazdroid.cmsgen.cmsgenerator.modifier.JsonFileModifier
import com.shazdroid.cmsgen.cmsgenerator.operations.Operations
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService
import java.io.File
import java.util.function.Supplier


class MainViewModel(
    private val project: Project
) {
    var keyStatuses: Map<String, KeyStatus> = emptyMap()

    private val fileServiceSupplier: Supplier<FileSelectionService> =
        CachedSingletonsRegistry.lazy { service<FileSelectionService>() }
    private val fileService = service<FileSelectionService>()
    private val fileModifier = FileModifier()
    private val jsonFileModifier = JsonFileModifier()

    private val operations: Lazy<Operations> = lazy {
        Operations(
            project = project,
            fileService = fileService,
            jsonFileModifier = jsonFileModifier,
            fileModifier = fileModifier
        )
    }


    fun refreshFile(project: Project?, filePath: String) {
        if (project == null) return

        val virtualFile: VirtualFile? = VirtualFileManager.getInstance().findFileByUrl("file://$filePath")
        if (virtualFile != null) {
            // Refresh the file in the VFS
            virtualFile.refresh(false, true)

            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
    }

    fun getEnglishJsonFile(): File? {
        return fileServiceSupplier.get().getSelectedFiles().firstOrNull { it.contains("DefaultEn.json") }
            ?.let { File(it) }
    }

    fun getArabicJsonFile(): File? {
        return fileServiceSupplier.get().getSelectedFiles().firstOrNull { it.contains("DefaultArabic.json") }
            ?.let { File(it) }
    }

    fun getCmsKeyMapperFile(): File? {
        return fileServiceSupplier.get().getSelectedFiles().firstOrNull { it.contains("CmsKeyMapper.kt") }
            ?.let { File(it) }
    }

    fun getKeysFromCmsKeyMapper(): Set<String> {
        val cmsKeyFile = getCmsKeyMapperFile()
        if (cmsKeyFile == null || !cmsKeyFile.exists()) {
            println("CmsKeyMapper.kt file not found")
            return emptySet()
        }

        return try {
            val content = cmsKeyFile.readText()
            parseKeysFromContent(content)
        } catch (e: Exception) {
            e.printStackTrace()
            emptySet()
        }
    }

    private fun parseKeysFromContent(content: String): Set<String> {
        val regex = Regex("""const\s+val\s+\w+\s*=\s*"([^"]+)"""")
        return regex.findAll(content).map { it.groupValues[1] }.toSet()
    }


    fun readJsonAsList(file: File?): List<Pair<String, String>> {
        if (file == null || !file.exists()) return emptyList()

        val objectMapper = ObjectMapper().registerModule(KotlinModule())
        val typeRef = object : TypeReference<Map<String, String>>() {}
        val jsonMap: Map<String, String> = objectMapper.readValue(file, typeRef)

        // Convert the map to a list of key-value pairs (lines)
        return jsonMap.toList()
    }


    fun addCmsString(
        cmsKey: String,
        engContent: String,
        arContent: String,
        insertAtLine: Int = 0,
        fileSelected: String = ""
    ): Boolean {
        operations.value.AddOperations(

        ).apply {
            return addCmString(cmsKey, engContent, arContent, insertAtLine, fileSelected)
        }
    }
}
