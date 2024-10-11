package com.shazdroid.cmsgen.cmsgenerator.keycomparison

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.shazdroid.cmsgen.cmsgenerator.modifier.FileModifier
import com.shazdroid.cmsgen.cmsgenerator.modifier.JsonFileModifier
import com.shazdroid.cmsgen.cmsgenerator.operations.Operations
import com.shazdroid.cmsgen.cmsgenerator.viewmodel.KeyStatus
import com.shazdroid.cmsgen.cmsgenerator.viewmodel.MainViewModel
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer


class KeyComparisonTable(
    private val table: JTable,
    private val enFile: File?,
    private val arFile: File?,
    private val cmsKeys: Set<String>,
    private val viewModel: MainViewModel,
    private val compareOperations: Operations.CompareOperations,
    private val project: Project,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) {

    private val keyStatuses: MutableMap<String, KeyStatus> = mutableMapOf()

    init {
        setupTable()
        addBadgeClickListener()
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

        val sortedData = data.sortedWith(compareByDescending<Array<Any?>> {
            val key = it[0] as String
            val status = keyStatuses[key] ?: return@compareByDescending false

            status.isDuplicatedInEn || status.isDuplicatedInAr || status.isMissingInCmsKeyMapper ||
                    status.isMissingInEn || status.isMissingInAr
        })

        val paginatedData = sortedData.drop(pageIndex * pageSize).take(pageSize)
        val columnNames = arrayOf("Key", "English Value", "Arabic Value")

        return PreparedTableData(
            data = paginatedData,
            columnNames = columnNames,
            keyStatuses = keyStatuses
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
        // Implement logic to get the last value for a key from the JSON file
        // For simplicity, return a placeholder or empty string
        return ""
    }

    private fun updateTable(data: List<Array<Any?>>, keyStatuses: Map<String, KeyStatus>) {
        SwingUtilities.invokeLater {
            val columnNames = arrayOf("Key", "English Value", "Arabic Value")

            val model = object : DefaultTableModel(data.toTypedArray(), columnNames) {
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
                            // Badge was clicked
                            val key = table.model.getValueAt(row, column) as? String ?: ""
                            handleBadgeClick(key, row)
                        }
                    }
                }
            }
        })
    }

    private fun handleBadgeClick(key: String, row: Int) {
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

    private fun refreshTableData() {
        // Re-prepare and update the table data
        setupTable()
    }

    private data class PreparedTableData(
        val data: List<Array<Any?>>,
        val columnNames: Array<String>,
        val keyStatuses: Map<String, KeyStatus>
    )

    private inner class KeyColumnRenderer(
        private val keyStatuses: Map<String, KeyStatus>
    ) : JPanel(), TableCellRenderer {

        private val keyLabel = JLabel()
        private val badgeLabel = JLabel()

        init {
            layout = BorderLayout()
            add(keyLabel, BorderLayout.CENTER)
            add(badgeLabel, BorderLayout.EAST)

            border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
            badgeLabel.horizontalAlignment = SwingConstants.RIGHT
        }

        override fun getTableCellRendererComponent(
            table: JTable, value: Any?, isSelected: Boolean,
            hasFocus: Boolean, row: Int, column: Int
        ): Component {
            val key = value as? String ?: ""
            val status = keyStatuses[key]

            keyLabel.text = key
            keyLabel.font = table.font

            if (status != null) {
                val (badgeText, badgeColor) = when {
                    status.isDuplicatedInEn || status.isDuplicatedInAr -> "D" to Color.YELLOW
                    status.isMissingInCmsKeyMapper -> "M" to Color.ORANGE
                    status.isMissingInEn || status.isMissingInAr -> "!" to Color.RED
                    else -> "" to null
                }

                if (badgeColor != null && badgeText.isNotEmpty()) {
                    badgeLabel.icon = createBadgeIcon(badgeText, badgeColor, table.font)
                    badgeLabel.text = ""
                    badgeLabel.toolTipText = when (badgeText) {
                        "D" -> "Duplicate key detected"
                        "M" -> "Key missing in CmsKeyMapper.kt"
                        "!" -> "Key missing in one of the JSON files"
                        else -> null
                    }
                } else {
                    badgeLabel.icon = null
                    badgeLabel.text = ""
                    badgeLabel.toolTipText = null
                }
            } else {
                badgeLabel.icon = null
                badgeLabel.text = ""
                badgeLabel.toolTipText = null
            }

            // Handle selection background and foreground
            if (isSelected) {
                background = table.selectionBackground
                keyLabel.foreground = table.selectionForeground
                badgeLabel.foreground = table.selectionForeground
            } else {
                background = table.background
                keyLabel.foreground = table.foreground
                badgeLabel.foreground = table.foreground
            }

            return this
        }

        fun getBadgeBounds(): Rectangle? {
            badgeLabel.doLayout()
            badgeLabel.validate()
            val badgeRect = badgeLabel.bounds
            return badgeRect
        }

        private fun createBadgeIcon(text: String, color: Color, font: Font): Icon {
            return object : Icon {
                private val diameter: Int

                init {
                    val adjustedFontSize = 9f
                    val padding = 4
                    val fm = Toolkit.getDefaultToolkit().getFontMetrics(font.deriveFont(Font.BOLD, adjustedFontSize))
                    val textWidth = fm.stringWidth(text)
                    val textHeight = fm.height
                    diameter = (maxOf(textWidth, textHeight) + padding).coerceIn(14, 18)
                }

                override fun getIconWidth(): Int = diameter
                override fun getIconHeight(): Int = diameter

                override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
                    if (g == null) return
                    val g2 = g.create() as Graphics2D

                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                    val badgeX = x
                    val badgeY = y + ((c?.height ?: 0) - diameter) / 2

                    g2.color = color
                    g2.fillOval(badgeX, badgeY, diameter, diameter)

                    g2.color = Color.WHITE
                    g2.font = font.deriveFont(Font.BOLD, 9f)
                    val fm = g2.fontMetrics
                    val textWidth = fm.stringWidth(text)
                    val textX = badgeX + (diameter - textWidth) / 2
                    val textY = badgeY + (diameter + fm.ascent - fm.descent) / 2 - 1

                    g2.drawString(text, textX, textY)
                    g2.dispose()
                }
            }
        }
    }
}

