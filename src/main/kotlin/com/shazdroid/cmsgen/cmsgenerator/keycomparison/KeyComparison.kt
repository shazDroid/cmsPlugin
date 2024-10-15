package com.shazdroid.cmsgen.cmsgenerator.keycomparison

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
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
import javax.swing.event.TableModelEvent
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

    private fun addSearchFunctionality(table: JTable, searchField: JTextField, renderer: KeyColumnRenderer) {
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

            val paginatedData = sortedData
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


    fun handleBadgeClick(key: String, project: Project, parentComponent: Component) {
        ApplicationManager.getApplication().invokeLater {
            val status = viewModel.keyStatuses[key]
            if (status == null) {
                Messages.showDialog(
                    parentComponent,
                    "Status for key '$key' not found.",
                    "Error",
                    arrayOf("OK"),
                    0,
                    Messages.getErrorIcon()
                )
                return@invokeLater
            }

            if (status.isDuplicatedInEn || status.isDuplicatedInAr) {
                val files = mutableListOf<String>()
                if (status.isDuplicatedInEn) files.add("English JSON")
                if (status.isDuplicatedInAr) files.add("Arabic JSON")
                val fileList = files.joinToString(" and ")

                val result = Messages.showDialog(
                    parentComponent,
                    "Key '$key' is duplicated in $fileList. Do you want to remove duplicates?",
                    "Duplicate Key Detected",
                    arrayOf("Yes", "No"),
                    0,
                    Messages.getWarningIcon()
                )

                if (result == 0) {  // YES Option
                    if (status.isDuplicatedInEn) {
                        compareOperations.removeDuplicatesInFile(
                            viewModel.getEnglishJsonFile(),
                            key,
                            project,
                            viewModel,
                            this
                        )
                    }
                    if (status.isDuplicatedInAr) {
                        compareOperations.removeDuplicatesInFile(
                            viewModel.getArabicJsonFile(),
                            key,
                            project,
                            viewModel,
                            this
                        )
                    }
                }
            } else if (status.isMissingInCmsKeyMapper) {
                val result = Messages.showDialog(
                    parentComponent,
                    "Key '$key' is missing in CmsKeyMapper.kt. Do you want to add it?",
                    "Add Key to CmsKeyMapper.kt",
                    arrayOf("Yes", "No"),
                    0,
                    Messages.getQuestionIcon()
                )
                if (result == 0) {  // YES Option
                    val fileModifier = FileModifier()
                    val cmsKeyFilePath = viewModel.getCmsKeyMapperFile()?.path ?: ""

                    if (cmsKeyFilePath.isNotEmpty()) {
                        val operationResult = fileModifier.appendCmsKeyToFile(cmsKeyFilePath, key, project)
                        if (operationResult == FileModifier.FileOperationResult.SUCCESS) {
                            // Refresh the table with proper English and Arabic values
                            val enValue = viewModel.getValueFromFile(viewModel.getEnglishJsonFile(), key)
                            val arValue = viewModel.getValueFromFile(viewModel.getArabicJsonFile(), key)
                            updateTableRowWithValues(key, enValue, arValue)
                            updateKeyStatusAfterOperation(key)
                            updateTableRowAfterOperation(key, enValue, arValue)
                        } else {
                            Messages.showDialog(
                                parentComponent,
                                "Error adding key '$key' to CmsKeyMapper.kt.",
                                "Error",
                                arrayOf("OK"),
                                0,
                                Messages.getErrorIcon()
                            )
                        }
                    } else {
                        Messages.showDialog(
                            parentComponent,
                            "Error: CmsKeyMapper.kt file not found.",
                            "Error",
                            arrayOf("OK"),
                            0,
                            Messages.getErrorIcon()
                        )
                    }
                }
            } else if (status.isMissingInEn || status.isMissingInAr) {
                val missingIn = mutableListOf<String>()
                if (status.isMissingInEn) missingIn.add("English")
                if (status.isMissingInAr) missingIn.add("Arabic")
                val files = missingIn.joinToString(" and ")

                val result = Messages.showDialog(
                    parentComponent,
                    "Key '$key' is missing in $files JSON file(s). Do you want to add it?",
                    "Add Key to JSON File(s)",
                    arrayOf("Yes", "No"),
                    0,
                    Messages.getQuestionIcon()
                )

                if (result == 0) {  // YES Option
                    val jsonModifier = JsonFileModifier()
                    val enFilePath = viewModel.getEnglishJsonFile()?.path ?: ""
                    val arFilePath = viewModel.getArabicJsonFile()?.path ?: ""

                    var enAdded = false
                    var arAdded = false

                    var enValue: String? = null
                    var arValue: String? = null

                    if (enFilePath.isNotEmpty() && status.isMissingInEn) {
                        enValue = Messages.showInputDialog(
                            project,
                            "Enter English value for '$key':",
                            "Add English Value",
                            Messages.getQuestionIcon()
                        )
                        if (enValue != null) {
                            jsonModifier.appendToEnglishJson(enFilePath, key, enValue, project)
                            enAdded = true
                        }
                    }

                    if (arFilePath.isNotEmpty() && status.isMissingInAr) {
                        arValue = Messages.showInputDialog(
                            project,
                            "Enter Arabic value for '$key':",
                            "Add Arabic Value",
                            Messages.getQuestionIcon()
                        )
                        if (arValue != null) {
                            jsonModifier.appendToArabicJson(arFilePath, key, arValue, project)
                            arAdded = true
                        }
                    }

                    if (enAdded || arAdded) {
                        updateKeyStatusAfterOperation(key, enAdded, arAdded)
                        updateTableRowAfterOperation(key, enValue, arValue)
                        Messages.showDialog(
                            parentComponent,
                            "Key '$key' added to JSON file(s).",
                            "Success",
                            arrayOf("OK"),
                            0,
                            Messages.getInformationIcon()
                        )
                    }
                }
            }
        }
    }


    fun reSortTable() {
        val rowSorter = table.rowSorter as? TableRowSorter<DefaultTableModel> ?: return
        rowSorter.sort()
    }


    fun updateTableRowAfterOperation(key: String, enValue: String?, arValue: String?) {
        val rowIndex = getRowIndexForKey(key)
        if (rowIndex != -1) {
            table.model.setValueAt(enValue, rowIndex, 1)
            table.model.setValueAt(arValue, rowIndex, 2)

            table.tableChanged(TableModelEvent(table.model, rowIndex))
        }
    }

    fun getRowIndexForKey(key: String): Int {
        for (rowIndex in 0 until table.rowCount) {
            val tableKey = table.getValueAt(rowIndex, 0) as? String
            if (tableKey == key) {
                return rowIndex
            }
        }
        return -1
    }

    fun updateTableRowWithValues(key: String, enValue: String?, arValue: String?) {
        val rowIndex = table.model.rowCount - 1

        for (i in 0 until table.model.rowCount) {
            val tableKey = table.model.getValueAt(i, 0) as? String
            if (tableKey == key) {
                table.model.setValueAt(enValue ?: "", i, 1)
                table.model.setValueAt(arValue ?: "", i, 2)

                (table.model as DefaultTableModel).fireTableRowsUpdated(i, i)
                break
            }
        }
    }

    fun updateKeyStatusAfterOperation(
        key: String,
        isEnglishUpdated: Boolean = false,
        isArabicUpdated: Boolean = false
    ) {
        val status = viewModel.keyStatuses[key] ?: return

        if (isEnglishUpdated) {
            status.isMissingInEn = false
        }
        if (isArabicUpdated) {
            status.isMissingInAr = false
        }
        status.isMissingInCmsKeyMapper = false

        viewModel.keyStatuses[key] = status
    }

    fun refreshTableData() {
        println("refreshTableData() called")
        keyStatuses.clear()
        SwingUtilities.invokeLater {
            setupTable()
        }
    }
}


