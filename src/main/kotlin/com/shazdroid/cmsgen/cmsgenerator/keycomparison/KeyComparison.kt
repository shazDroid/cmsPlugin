package com.shazdroid.cmsgen.cmsgenerator.keycomparison

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.google.gson.JsonElement
import com.google.gson.JsonParser
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
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

class KeyComparisonTable(
    private val table: JTable,
    private val enFile: File?,
    private val arFile: File?,
    private val viewModel: MainViewModel,
    private val compareOperations: Operations.CompareOperations,
    private val project: Project, private val progressBar: JProgressBar,
    private val searchTextField: JTextField,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) {

    private val keyStatuses: MutableMap<String, KeyStatus> = mutableMapOf()

    init {
        setupTable()
        addBadgeClickListener()
    }

    fun addSearchFunctionality(table: JTable, searchField: JTextField, renderer: KeyColumnRenderer) {
        // Ensure the table has a row sorter
        val rowSorter = table.rowSorter as? TableRowSorter<*> ?: run {
            println("TableRowSorter not found. Assigning a new TableRowSorter.")
            val sorter = TableRowSorter<TableModel>(table.model)
            table.rowSorter = sorter
            sorter
        }

        // Add a DocumentListener to respond to changes in the search field
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                updateFilter()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                updateFilter()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                updateFilter()
            }

            private fun updateFilter() {
                val text = searchField.text.trim()
                renderer.searchText = text // Update the renderer's searchText

                if (text.isEmpty()) {
                    rowSorter.setRowFilter(null)
                } else {
                    // Filter based on multiple columns: "Key" (0), "English Value" (1), "Arabic Value" (2)
                    rowSorter.setRowFilter(RowFilter.regexFilter("(?i)$text", 0, 1, 2))
                }

                if (rowSorter.rowFilter != null && table.rowCount == 0 && text.isNotEmpty()) {
                    JOptionPane.showMessageDialog(
                        table,
                        "No matches found for \"$text\".",
                        "No Results",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            }
        })
    }

    private fun setupTable(pageSize: Int = 50, pageIndex: Int = 0) {
        scope.launch {
            // Show the progress bar before starting
            withContext(Dispatchers.Main) {
                progressBar.isVisible = true
                progressBar.value = 0
            }

            val preparedData = withContext(Dispatchers.IO) {
                prepareTableData(pageSize, pageIndex)
            }

            updateTable(preparedData.data, preparedData.keyStatuses)

            // Hide the progress bar after loading
            withContext(Dispatchers.Main) {
                progressBar.isVisible = false
            }
        }
    }

    private suspend fun prepareTableData(pageSize: Int, pageIndex: Int): PreparedTableData {
        try {


        val enKeyOccurrences = collectKeyOccurrences(enFile)
        val arKeyOccurrences = collectKeyOccurrences(arFile)

            val cmsKeys = withContext(Dispatchers.IO) {
                viewModel.getKeysFromCmsKeyMapper()
            }

        val allKeys = enKeyOccurrences.keys.union(arKeyOccurrences.keys).union(cmsKeys)
        val data = mutableListOf<Array<Any?>>()

            val totalKeys = allKeys.size
            var processedKeys = 0

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

            val enValue = withContext(Dispatchers.IO) { getLastValueForKey(enFile, key) }
            val arValue = withContext(Dispatchers.IO) { getLastValueForKey(arFile, key) }

            println("Key: $key, enValue: $enValue, arValue: $arValue")

            data.add(arrayOf(key, enValue, arValue))

            processedKeys++

            val progress = (processedKeys * 100) / totalKeys

            // Update the progress bar on the EDT
            withContext(Dispatchers.Main) {
                progressBar.value = progress
            }

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

        } catch (e: Exception) {
            return PreparedTableData(
                data = emptyArray(),
                columnNames = arrayOf("Key", "English Value", "Arabic Value"),
                keyStatuses = keyStatuses,
                emptySet()
            )
        }
    }


    private fun collectKeyOccurrences(file: File?): Map<String, Int> {
        if (file == null || !file.exists()) return emptyMap()

        val keyOccurrences = mutableMapOf<String, Int>()
        val keyStack = mutableListOf<String>()

        try {
            val jsonFactory = JsonFactory()
            val parser = jsonFactory.createParser(file)

            var currentKey: String? = null

            while (!parser.isClosed) {
                val token = parser.nextToken()
                when (token) {
                    JsonToken.FIELD_NAME -> {
                        currentKey = parser.currentName
                        keyStack.add(currentKey)

                        val fullKey = currentKey

                        // Increment the count for the key
                        keyOccurrences[fullKey] = keyOccurrences.getOrDefault(fullKey, 0) + 1
                    }

                    JsonToken.START_OBJECT, JsonToken.START_ARRAY -> {

                    }

                    JsonToken.END_OBJECT, JsonToken.END_ARRAY -> {
                        if (keyStack.isNotEmpty()) {
                            keyStack.removeAt(keyStack.size - 1)
                        }
                    }

                    else -> {

                    }
                }
            }

            parser.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return keyOccurrences
    }

    private fun getLastValueForKey(file: File?, key: String): String {
        if (file == null || !file.exists()) return ""

        return try {
            val jsonString = file.readText()
            val jsonElement = JsonParser.parseString(jsonString)
            findValueForKey(jsonElement, key) ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun findValueForKey(element: JsonElement, key: String): String? {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has(key)) {
                val valueElement = obj.get(key)
                return if (valueElement.isJsonPrimitive) {
                    valueElement.asString
                } else {
                    valueElement.toString()
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

            // Initialize TableRowSorter
            val sorter = TableRowSorter<TableModel>(model)
            table.rowSorter = sorter

            // Apply the custom renderer to the key column
            val keyRenderer = KeyColumnRenderer(keyStatuses)
            table.columnModel.getColumn(0).cellRenderer = keyRenderer

            val centerRenderer = DefaultTableCellRenderer().apply {
                horizontalAlignment = SwingConstants.CENTER
            }
            table.columnModel.getColumn(1).cellRenderer = centerRenderer
            table.columnModel.getColumn(2).cellRenderer = centerRenderer

            table.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS

            val keyRendererSearch = table.getColumnModel().getColumn(0).cellRenderer as? KeyColumnRenderer
                ?: throw IllegalStateException("KeyColumnRenderer not assigned to the 'Key' column.")

            addSearchFunctionality(table, searchTextField, keyRendererSearch)

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

                    // Calculate badge bounds
                    val badgeBounds = rendererComponent.calculateBadgeBounds(cellRect)
                    println("Badge bounds for row $row: $badgeBounds")

                    // Adjust badge bounds relative to table coordinates
                    val adjustedBadgeBounds = Rectangle(
                        cellRect.x + badgeBounds.x,
                        cellRect.y + badgeBounds.y,
                        badgeBounds.width,
                        badgeBounds.height
                    )
                    println("Adjusted badge bounds: $adjustedBadgeBounds")

                    if (adjustedBadgeBounds.contains(e.x, e.y)) {
                        val key = table.model.getValueAt(row, column) as? String ?: ""
                        println("Badge clicked for key: $key")
                        handleBadgeClick(key, project, table)
                    }
                }
            }
        })
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
                        compareOperations.removeDuplicatesInFile(
                            viewModel.getEnglishJsonFile(), key, project, viewModel
                        )
                    }
                    if (status.isDuplicatedInAr) {
                        compareOperations.removeDuplicatesInFile(viewModel.getArabicJsonFile(), key, project, viewModel)
                    }
                    JOptionPane.showMessageDialog(parentComponent, "Duplicates for key '$key' have been removed.")
                    SwingUtilities.invokeLater {
                        refreshTableData()
                    }
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
                            SwingUtilities.invokeLater {
                                refreshTableData()
                            }
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
                        SwingUtilities.invokeLater {
                            refreshTableData()
                        }
                    }
                }
            }
        }
    }


    fun refreshTableData() {
        println("refreshTableData() called")
        // Clear any cached data
        keyStatuses.clear()

        SwingUtilities.invokeLater {
            setupTable()
        }
    }
}


