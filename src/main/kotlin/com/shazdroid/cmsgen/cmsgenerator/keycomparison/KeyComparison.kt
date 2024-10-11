package com.shazdroid.cmsgen.cmsgenerator.keycomparison

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.shazdroid.cmsgen.cmsgenerator.custom_guis.KeyColumnRenderer
import com.shazdroid.cmsgen.cmsgenerator.modifier.FileModifier
import com.shazdroid.cmsgen.cmsgenerator.modifier.JsonFileModifier
import com.shazdroid.cmsgen.cmsgenerator.operations.Operations
import com.shazdroid.cmsgen.cmsgenerator.viewmodel.KeyStatus
import com.shazdroid.cmsgen.cmsgenerator.viewmodel.MainViewModel
import com.shazdroid.cmsgen.cmsgenerator.viewmodel.PreparedTableData
import kotlinx.coroutines.*
import java.awt.Component
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.regex.Pattern
import javax.swing.JOptionPane
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class KeyComparisonTable(
    private val table: JTable,
    private val enFile: File?,
    private val arFile: File?,
    private val cmsKeys: Set<String>,
    private val viewModel: MainViewModel,
    private val compareOperations: Operations.CompareOperations,
    private val project: Project,
    private val handleBadgeClick: (String, Project, Component) -> Unit,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) {

    private val keyStatuses: MutableMap<String, KeyStatus> = mutableMapOf()

    // Caching the JSON maps
    private var enJsonMap: Map<String, String>? = null
    private var arJsonMap: Map<String, String>? = null


    init {
        setupTable()
        addBadgeClickListener()
    }

    private fun parseJsonFile(file: File?): Map<String, String> {
        if (file == null || !file.exists()) return emptyMap()
        println("Reading file: ${file.path}")
        return try {
            val jsonString = file.readText()
            val type = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson(jsonString, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    private fun setupTable(pageSize: Int = 50, pageIndex: Int = 0) {
        scope.launch {
            val preparedData = withContext(Dispatchers.IO) {
                prepareTableData(pageSize, pageIndex)
            }

            updateTable(preparedData.data, preparedData.keyStatuses)
        }
    }

    private suspend fun prepareTableData(pageSize: Int, pageIndex: Int): PreparedTableData {
        val enKeyOccurrences = collectKeyOccurrences(enFile)
        val arKeyOccurrences = collectKeyOccurrences(arFile)

        val allKeys = enKeyOccurrences.keys.union(arKeyOccurrences.keys).union(cmsKeys)
        val data = mutableListOf<Array<Any?>>()

        allKeys.forEach { key ->
            val enCount = enKeyOccurrences.getOrDefault(key, 0)
            val arCount = arKeyOccurrences.getOrDefault(key, 0)
            val cmsExists = cmsKeys.contains(key)

            val isDuplicatedInEn = enCount > 1
            val isDuplicatedInAr = arCount > 1
            val isMissingInEn = enCount == 0 && cmsExists
            val isMissingInAr = arCount == 0 && cmsExists
            val isMissingInCmsKeyMapper = !cmsExists

            val status = KeyStatus(
                enCount = enCount,
                arCount = arCount,
                inCmsKeyMapper = cmsExists,
                isDuplicatedInEn = isDuplicatedInEn,
                isDuplicatedInAr = isDuplicatedInAr,
                isMissingInEn = isMissingInEn,
                isMissingInAr = isMissingInAr,
                isMissingInCmsKeyMapper = isMissingInCmsKeyMapper
            )

            keyStatuses[key] = status

            val enValue = getLastValueForKey(enFile, key)
            val arValue = getLastValueForKey(arFile, key)

            data.add(arrayOf(key, enValue, arValue))
        }

        // Sorting and pagination
        val sortedData = data.sortedWith(compareByDescending {
            val key = it[0] as String
            val status = keyStatuses[key] ?: return@compareByDescending false

            status.isDuplicatedInEn || status.isDuplicatedInAr || status.isMissingInCmsKeyMapper ||
                    status.isMissingInEn || status.isMissingInAr
        })

        val paginatedData = sortedData.drop(pageIndex * pageSize).take(pageSize)
        val dataArray: Array<Array<Any?>> = paginatedData.toTypedArray()

        viewModel.keyStatuses = keyStatuses

        return PreparedTableData(
            data = dataArray,
            columnNames = arrayOf("Key", "English Value", "Arabic Value"),
            keyStatuses = keyStatuses,
            cmsKeys
        )
    }


    private fun collectKeyOccurrences(file: File?): Map<String, Int> {
        if (file == null || !file.exists()) return emptyMap()

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

    private fun getLastValueForKey(file: File?, key: String): String {
        val map = when (file) {
            enFile -> {
                if (enJsonMap == null) {
                    enJsonMap = parseJsonFile(enFile)
                    println("enJsonMap reloaded")
                }
                enJsonMap
            }
            arFile -> {
                if (arJsonMap == null) {
                    arJsonMap = parseJsonFile(arFile)
                    println("arJsonMap reloaded")
                }
                arJsonMap
            }
            else -> null
        }
        val value = map?.get(key) ?: ""
        println("Retrieved value for key '$key' from ${file?.name}: '$value'")
        return map?.get(key) ?: ""
    }

    private fun findValueForKey(element: JsonElement, key: String): String? {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has(key)) {
                val valueElement = obj.get(key)
                if (valueElement.isJsonPrimitive) {
                    return valueElement.asString
                } else {
                    return valueElement.toString()
                }
            } else {
                for ((_, value) in obj.entrySet()) {
                    val result = findValueForKey(value, key)
                    if (result != null) {
                        return result
                    }
                }
            }
        } else if (element.isJsonArray) {
            val arr = element.asJsonArray
            for (item in arr) {
                val result = findValueForKey(item, key)
                if (result != null) {
                    return result
                }
            }
        }
        return null
    }

    private fun updateTable(data: Array<Array<Any?>>, keyStatuses: Map<String, KeyStatus>) {
        SwingUtilities.invokeLater {
            println("Updating table with new data")
            val columnNames = arrayOf("Key", "English Value", "Arabic Value")

            val model = object : DefaultTableModel(data, columnNames) {
                override fun isCellEditable(row: Int, column: Int): Boolean = false
                override fun getColumnClass(columnIndex: Int): Class<*> = String::class.java
            }

            table.model = model

            // Apply the custom renderer to the key column
            val keyRenderer = KeyColumnRenderer(keyStatuses)
            table.columnModel.getColumn(0).cellRenderer = keyRenderer

            // Center-align the other columns
            val centerRenderer = DefaultTableCellRenderer().apply {
                horizontalAlignment = SwingConstants.CENTER
            }
            table.columnModel.getColumn(1).cellRenderer = centerRenderer
            table.columnModel.getColumn(2).cellRenderer = centerRenderer

            table.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS

            table.revalidate()
            table.repaint()
        }
    }

    private fun addBadgeClickListener() {
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val point = e.point
                val row = table.rowAtPoint(point)
                val column = table.columnAtPoint(point)

                if (row == -1 || column != 0) {
                    return
                }

                val cellRect = table.getCellRect(row, column, false)
                val rendererComponent = table.getCellRenderer(row, column).getTableCellRendererComponent(
                    table, table.getValueAt(row, column), false, false, row, column
                )

                if (rendererComponent is KeyColumnRenderer) {
                    val badgeBounds = rendererComponent.getBadgeBounds()
                    if (badgeBounds != null) {
                        val adjustedBadgeBounds = Rectangle(
                            cellRect.x + badgeBounds.x,
                            cellRect.y + badgeBounds.y,
                            badgeBounds.width,
                            badgeBounds.height
                        )

                        if (adjustedBadgeBounds.contains(e.x, e.y)) {
                            val key = table.model.getValueAt(row, column) as? String ?: ""
                            handleBadgeClick(key, project, table)
                        }
                    }
                }
            }
        })
    }

    private fun handleBadgeClick(key: String) {
        val parentComponent = table
        handleBadgeClick(key, project, parentComponent)
    }

    // Your existing handleBadgeClick function
    fun handleBadgeClick(key: String, project: Project, parentComponent: Component) {
        ApplicationManager.getApplication().invokeLater {
            val status = viewModel.keyStatuses[key]
            if (status == null) {
                JOptionPane.showMessageDialog(
                    parentComponent,
                    "Status for key '$key' not found.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
                return@invokeLater
            }

            if (status.isDuplicatedInEn || status.isDuplicatedInAr) {
                val files = mutableListOf<String>()
                if (status.isDuplicatedInEn) files.add("English JSON")
                if (status.isDuplicatedInAr) files.add("Arabic JSON")
                val fileList = files.joinToString(" and ")

                val result = JOptionPane.showConfirmDialog(
                    parentComponent,
                    "Key '$key' is duplicated in $fileList. Do you want to remove duplicates?",
                    "Duplicate Key Detected",
                    JOptionPane.YES_NO_OPTION
                )

                if (result == JOptionPane.YES_OPTION) {
                    if (status.isDuplicatedInEn) {
                        compareOperations.removeDuplicatesInFile(viewModel.getEnglishJsonFile(), key, project)
                    }
                    if (status.isDuplicatedInAr) {
                        compareOperations.removeDuplicatesInFile(viewModel.getArabicJsonFile(), key, project)
                    }
                    JOptionPane.showMessageDialog(parentComponent, "Duplicates for key '$key' have been removed.")
                    refreshTableData()
                }
            } else if (status.isMissingInCmsKeyMapper) {
                val result = JOptionPane.showConfirmDialog(
                    parentComponent,
                    "Key '$key' is missing in CmsKeyMapper.kt. Do you want to add it?",
                    "Add Key to CmsKeyMapper.kt",
                    JOptionPane.YES_NO_OPTION
                )
                if (result == JOptionPane.YES_OPTION) {
                    val fileModifier = FileModifier()
                    val cmsKeyFilePath = viewModel.getCmsKeyMapperFile()?.path ?: ""

                    if (cmsKeyFilePath.isNotEmpty()) {
                        val operationResult = fileModifier.appendCmsKeyToFile(cmsKeyFilePath, key, project)
                        if (operationResult == FileModifier.FileOperationResult.SUCCESS) {
                            JOptionPane.showMessageDialog(parentComponent, "Key '$key' added to CmsKeyMapper.kt.")
                            refreshTableData()
                        } else {
                            JOptionPane.showMessageDialog(
                                parentComponent,
                                "Error adding key '$key' to CmsKeyMapper.kt."
                            )
                        }
                    } else {
                        JOptionPane.showMessageDialog(parentComponent, "Error: CmsKeyMapper.kt file not found.")
                    }
                }
            } else if (status.isMissingInEn || status.isMissingInAr) {
                val missingIn = mutableListOf<String>()
                if (status.isMissingInEn) missingIn.add("English")
                if (status.isMissingInAr) missingIn.add("Arabic")
                val files = missingIn.joinToString(" and ")

                val result = JOptionPane.showConfirmDialog(
                    parentComponent,
                    "Key '$key' is missing in $files JSON file(s). Do you want to add it?",
                    "Add Key to JSON File(s)",
                    JOptionPane.YES_NO_OPTION
                )

                if (result == JOptionPane.YES_OPTION) {
                    val jsonModifier = JsonFileModifier()
                    val enFilePath = viewModel.getEnglishJsonFile()?.path ?: ""
                    val arFilePath = viewModel.getArabicJsonFile()?.path ?: ""

                    var enAdded = false
                    var arAdded = false

                    if (enFilePath.isNotEmpty() && status.isMissingInEn) {
                        val enValue = JOptionPane.showInputDialog(parentComponent, "Enter English value for '$key':")
                        if (enValue != null) {
                            jsonModifier.appendToEnglishJson(enFilePath, key, enValue, project)
                            enAdded = true
                        }
                    }

                    if (arFilePath.isNotEmpty() && status.isMissingInAr) {
                        val arValue = JOptionPane.showInputDialog(parentComponent, "Enter Arabic value for '$key':")
                        if (arValue != null) {
                            jsonModifier.appendToArabicJson(arFilePath, key, arValue, project)
                            arAdded = true
                        }
                    }

                    if (enAdded || arAdded) {
                        JOptionPane.showMessageDialog(parentComponent, "Key '$key' added to JSON file(s).")
                        refreshTableData()
                    }
                }
            }
        }
    }

    fun refreshTableData() {
        println("refreshTableData() called")
        // Clear any cached data
        keyStatuses.clear()
        enJsonMap = null
        arJsonMap = null

        SwingUtilities.invokeLater {
            setupTable()
        }
    }
}


