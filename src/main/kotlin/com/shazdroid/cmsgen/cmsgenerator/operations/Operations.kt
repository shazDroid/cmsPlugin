package com.shazdroid.cmsgen.cmsgenerator.operations

import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.shazdroid.cmsgen.cmsgenerator.custom_guis.KeyColumnRenderer
import com.shazdroid.cmsgen.cmsgenerator.modifier.FileModifier
import com.shazdroid.cmsgen.cmsgenerator.modifier.JsonFileModifier
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService
import com.shazdroid.cmsgen.cmsgenerator.viewmodel.KeyStatus
import com.shazdroid.cmsgen.cmsgenerator.viewmodel.MainViewModel
import com.shazdroid.cmsgen.cmsgenerator.viewmodel.PreparedTableData
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import javax.swing.JOptionPane
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import kotlin.coroutines.CoroutineContext

class Operations(
    private val project: Project,
    private val fileService: FileSelectionService,
    private val fileModifier: FileModifier,
    private val jsonFileModifier: JsonFileModifier,
    private val viewModel: MainViewModel? = null
) {

    inner class AddOperations {
        fun addCmString(
            cmsKey: String,
            engContent: String,
            arContent: String,
            insertAtLine: Int = 0,
            selectedFile: String = ""
        ): Boolean {
            var isSuccess = false

            if (cmsKey.isNotEmpty() && engContent.isNotEmpty() && arContent.isNotEmpty()) {
                fileService.getSelectedFiles().first()
                fileService.getSelectedFiles().forEachIndexed { index, item ->
                    if (item.contains("CmsKeyMapper.kt")) {
                        when (fileModifier.appendCmsKeyToFile(item, cmsKey.trim(), project)) {
                            FileModifier.FileOperationResult.SUCCESS -> {
                                isSuccess = true
                            }

                            FileModifier.FileOperationResult.FILE_NOT_FOUND -> {
                                isSuccess = false
                            }

                            FileModifier.FileOperationResult.DUPLICATE_KEY -> {
                                isSuccess = false
                            }

                            FileModifier.FileOperationResult.COMPANION_OBJECT_NOT_FOUND -> {
                                isSuccess = false
                            }

                            FileModifier.FileOperationResult.WRITE_ERROR -> {
                                isSuccess = false
                            }
                        }
                    }
                }

                if (isSuccess) {
                    fileService.getSelectedFiles().forEachIndexed { _, item ->
                        if (item.contains("DefaultEn.json")) {
                            jsonFileModifier.appendToEnglishJson(
                                item,
                                cmsKey,
                                engContent.trim(),
                                project,
                                insertAtLine
                            )
                        }

                        if (item.contains("DefaultArabic.json")) {
                            jsonFileModifier.appendToArabicJson(
                                item, cmsKey, arContent.trim(), project,
                                insertAtLine
                            )
                        }
                    }
                }

                if (isSuccess) {
                    Messages.showMessageDialog("Cms key added successfully", "Success", Messages.getInformationIcon())
                    return true
                }

            } else {
                Messages.showMessageDialog("Please fill in all three inputs.", "Error", Messages.getErrorIcon())
                return false
            }

            return false
        }
    }


    inner class CompareOperations(private val table: JTable) : CoroutineScope {
        private val job = Job()
        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Main + job

        // Function to collect key occurrences from a file using coroutines
        private fun collectKeyOccurrences(file: File?): Map<String, Int> {
            if (file == null || !file.exists()) {
                return emptyMap()
            }

            val keyOccurrences = mutableMapOf<String, Int>()
            val keyPattern = Pattern.compile("\"(.*?)\"\\s*:")

            file.forEachLine { line ->
                val matcher = keyPattern.matcher(line)
                while (matcher.find()) {
                    val key = matcher.group(1)
                    keyOccurrences[key] = keyOccurrences.getOrDefault(key, 0) + 1
                }
            }
            return keyOccurrences
        }


        // Function to remove duplicates using coroutines
        fun removeDuplicatesInFile(file: File?, key: String, project: Project) {
            if (file == null || !file.exists()) {
                JOptionPane.showMessageDialog(null, "File not found.")
                return
            }

            launch {
                val objectMapper = ObjectMapper().registerModule(KotlinModule())
                val typeRef = object : TypeReference<MutableMap<String, String>>() {}
                val content: MutableMap<String, String> = withContext(Dispatchers.IO) {
                    objectMapper.readValue(file, typeRef)
                }

                // Check if the key exists in the map and handle duplicates
                if (content.containsKey(key)) {
                    val value = content[key]
                    val cleanedMap = mutableMapOf<String, String>()

                    withContext(Dispatchers.IO) {
                        val lines = file.readLines()
                        for (line in lines) {
                            val matcher = Pattern.compile("\"(.*?)\"\\s*:\\s*\"(.*?)\"").matcher(line)
                            if (matcher.find()) {
                                val lineKey = matcher.group(1)
                                val lineValue = matcher.group(2)
                                if (lineKey != key) {
                                    cleanedMap[lineKey] = lineValue
                                }
                            }
                        }
                        cleanedMap[key] = value!!

                        // Write the cleaned map back to the file
                        file.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cleanedMap))
                        viewModel?.refreshFile(project, file.path)
                    }

                    JOptionPane.showMessageDialog(null, "Duplicates removed for key: $key.")
                }
            }
        }

        // Function to get the last value for a key from a file using coroutines
        private fun getLastValueForKey(file: File?, targetKey: String): String? {
            if (file == null || !file.exists()) return ""

            val mapper = ObjectMapper()
            val parser = mapper.factory.createParser(file)
            var lastValue: String? = null

            try {
                while (!parser.isClosed) {
                    val token = parser.nextToken()
                    if (token == JsonToken.FIELD_NAME) {
                        val fieldName = parser.currentName
                        parser.nextToken() // Move to the value
                        if (fieldName == targetKey) {
                            val node = parser.readValueAsTree<JsonNode>()
                            lastValue = node.toString()
                        } else {
                            parser.skipChildren()
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                parser.close()
            }
            return "Value for $lastValue"
        }

        // Function to prepare data for the table using coroutines and pagination
        fun prepareTableData(pageSize: Int = 50, pageIndex: Int = 0): PreparedTableData
        //withContext(Dispatchers.IO)
        {
            val enFile = viewModel?.getEnglishJsonFile()
            val arFile = viewModel?.getArabicJsonFile()
            val cmsKeys = viewModel?.getKeysFromCmsKeyMapper() ?: emptySet()

            val enKeyOccurrences = collectKeyOccurrences(enFile)
            val arKeyOccurrences = collectKeyOccurrences(arFile)

            val allKeys = enKeyOccurrences.keys.union(arKeyOccurrences.keys).union(cmsKeys)
            val data = mutableListOf<Array<Any?>>()
            val keyStatuses = mutableMapOf<String, KeyStatus>()

            allKeys.forEach { key ->
                val enCount = enKeyOccurrences.getOrDefault(key, 0)
                val arCount = arKeyOccurrences.getOrDefault(key, 0)
                val cmsExists = cmsKeys.contains(key)

                val isDuplicatedInEn = enCount > 1
                val isDuplicatedInAr = arCount > 1
                val isMissingInEn = enCount == 0 && cmsExists
                val isMissingInAr = arCount == 0 && cmsExists
                val isMissingInCmsKeyMapper = !cmsExists

                keyStatuses[key] = KeyStatus(
                    enCount,
                    arCount,
                    cmsExists,
                    isDuplicatedInEn,
                    isDuplicatedInAr,
                    isMissingInEn,
                    isMissingInAr,
                    isMissingInCmsKeyMapper
                )

                val enValue = getLastValueForKey(enFile, key)
                val arValue = getLastValueForKey(arFile, key)

                data.add(arrayOf(key, enValue, arValue))
            }

            val sortedData = data.sortedWith(compareByDescending<Array<Any?>> {
                val key = it[0] as String
                val status = keyStatuses[key] ?: return@compareByDescending false

                status.isDuplicatedInEn || status.isDuplicatedInAr || status.isMissingInCmsKeyMapper ||
                        status.isMissingInEn || status.isMissingInAr
            })

            val paginatedData = sortedData.drop(pageIndex * pageSize).take(pageSize)

            val columnNames = arrayOf("Key", "English Value", "Arabic Value")

            return PreparedTableData(
                data = paginatedData.toTypedArray(),
                columnNames = columnNames,
                keyStatuses = keyStatuses,
                cmsKeys = cmsKeys
            )
        }


        // Function to safely update the table
        fun safeUpdateTable(data: List<Array<Any?>>, keyStatuses: Map<String, KeyStatus>, cmsKeys: Set<String>) {
            SwingUtilities.invokeLater {
                updateTable(data, keyStatuses, cmsKeys)
            }
        }


        // Function to update the table UI with the prepared data
        fun updateTable(data: List<Array<Any?>>, keyStatuses: Map<String, KeyStatus>, cmsKeys: Set<String>) {
            SwingUtilities.invokeLater {
                val columnNames = arrayOf("Key", "English Value", "Arabic Value")

                val model = object : DefaultTableModel(data.toTypedArray(), columnNames) {
                    override fun isCellEditable(row: Int, column: Int): Boolean = false
                    override fun getColumnClass(columnIndex: Int): Class<*> = String::class.java
                }

                table.model = model

                // Reapply the KeyColumnRenderer
                val keyRenderer = KeyColumnRenderer(keyStatuses)
                table.columnModel.getColumn(0).cellRenderer = keyRenderer

                // Center-align the English and Arabic value columns
                val centerRenderer = DefaultTableCellRenderer().apply {
                    horizontalAlignment = SwingConstants.CENTER
                }
                table.columnModel.getColumn(1).cellRenderer = centerRenderer
                table.columnModel.getColumn(2).cellRenderer = centerRenderer

                table.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS

                table.model.addTableModelListener {
                    SwingUtilities.invokeLater {
                        val keyRenderer = KeyColumnRenderer(viewModel?.keyStatuses!!)
                        table.columnModel.getColumn(0).cellRenderer = keyRenderer
                        table.repaint()
                    }
                }
            }
        }


        fun applyPersistentRenderer(keyStatuses: Map<String, KeyStatus>) {
            val keyColumnRenderer = KeyColumnRenderer(keyStatuses)
            // Apply the renderer to the key column


            // Add a listener to reapply the renderer if the table structure changes
            table.model.addTableModelListener {
                SwingUtilities.invokeLater {
                    table.columnModel.getColumn(0).cellRenderer = keyColumnRenderer
                    table.revalidate()
                    table.repaint()
                }
            }
        }


        fun cancelOperations() {
            job.cancel() // Cancels all ongoing operations
        }
    }

}