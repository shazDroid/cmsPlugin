package com.shazdroid.cmsgen.cmsgenerator.window

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.shazdroid.cmsgen.cmsgenerator.custom_guis.KeyColumnRenderer
import com.shazdroid.cmsgen.cmsgenerator.keycomparison.KeyComparisonTable
import com.shazdroid.cmsgen.cmsgenerator.modifier.FileModifier
import com.shazdroid.cmsgen.cmsgenerator.modifier.JsonFileModifier
import com.shazdroid.cmsgen.cmsgenerator.operations.Operations
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService
import com.shazdroid.cmsgen.cmsgenerator.util.*
import com.shazdroid.cmsgen.cmsgenerator.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Component
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel


class CmsMainWindow(private val project: Project) : JDialog() {
    // UI Components
    lateinit var mainPanel: JPanel
    lateinit var mainTabLayout: JTabbedPane
    lateinit var txtCmsKey: JTextArea
    lateinit var txtEnglishContent: JTextArea
    lateinit var txtArabicContent: JTextArea
    lateinit var txtInsertAtLine: JFormattedTextField
    lateinit var chkInsertAtLine: JCheckBox
    lateinit var chkAddMultiple: JCheckBox
    lateinit var addButton: JButton
    lateinit var clearButton: JButton
    lateinit var txtTableSearch: JTextField
    lateinit var jscrollInsertAtLine: JScrollPane
    lateinit var tblInsertAtLine: JTable
    lateinit var txtReferenceFile: JLabel
    lateinit var cmbReferenceFile: JComboBox<String>
    lateinit var lblEnterLineNo: JLabel
    lateinit var addCmsStringPanel: JPanel

    // Compare components
    lateinit var comparePanel: JPanel
    lateinit var compareJScrollPane: JScrollPane
    lateinit var comparisonTable: JTable
    lateinit var txtSearchField: JTextField
    lateinit var lblSearch: JLabel
    lateinit var refreshLabel: JLabel

    private val uiScope = CoroutineScope(Dispatchers.Main)

    private val frame = JFrame()
    private val fileService = service<FileSelectionService>()
    private val fileModifier = FileModifier()
    private val jsonFileModifier = JsonFileModifier()
    private val viewModel = MainViewModel(project)

    private val operations: Lazy<Operations> = lazy {
        Operations(
            project = project,
            fileService = fileService,
            jsonFileModifier = jsonFileModifier,
            fileModifier = fileModifier,
            viewModel = viewModel
        )
    }

    private val compareOperations = operations.value.CompareOperations(comparisonTable)
    private val addOperations = operations.value.AddOperations()

    private val englishEntries = viewModel.readJsonAsList(viewModel.getEnglishJsonFile())
    private val arabicEntries = viewModel.readJsonAsList(viewModel.getArabicJsonFile())
    private var isComparisonDataLoaded = false

    private var keyComparisonTable: KeyComparisonTable? = null

    // Show form
    fun showForm() {
        generateFrame()
        initUi()
        handleAddCmsString()
        addIconsToTab()
        handleClear()
        tabOperations()
    }

    private fun compareOperations() {
        refreshLabel.icon = IconLoader.getIcon("refresh.svg".icon(), javaClass)
        refreshLabel.onClick {
            uiScope.launch {
                SwingUtilities.invokeLater {
                    keyComparisonTable?.refreshTableData()
                }
            }
        }
    }

    private fun loadDataForComparison() {
        val enFile = viewModel.getEnglishJsonFile()
        val arFile = viewModel.getArabicJsonFile()
        val cmsKeys = viewModel.getKeysFromCmsKeyMapper()
        // Create an instance of KeyComparisonTable
        keyComparisonTable = KeyComparisonTable(
            table = comparisonTable,
            enFile = enFile,
            arFile = arFile,
            cmsKeys = cmsKeys,
            viewModel = viewModel,
            compareOperations = compareOperations,
            project = project,
            handleBadgeClick = this::handleBadgeClick
        )
        isComparisonDataLoaded = true
    }

    private fun addIconsToTab() {
        mainTabLayout.setIconAt(0, IconLoader.getIcon("add_cms_string.svg".icon(), javaClass))
        mainTabLayout.setIconAt(1, IconLoader.getIcon("compare.svg".icon(), javaClass))
        mainTabLayout.setIconAt(2, IconLoader.getIcon("bulk.svg".icon(), javaClass))
        mainTabLayout.setIconAt(3, IconLoader.getIcon("setting.svg".icon(), javaClass))
    }

    private fun generateFrame() {
        frame.title = "CMS Input"
        frame.contentPane = mainPanel
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.pack()
    }

    private fun initUi() {
        showInsertAtLineFeature(false)
        setupTextArea()
        populateInsertAtLineCombo()
        compareOperations()
    }

    private fun tabOperations() {
        mainTabLayout.addChangeListener {
            if (mainTabLayout.selectedIndex == 1) {
                if (!isComparisonDataLoaded) loadDataForComparison()
            }
        }
    }

    private fun setupTextArea() {
        txtCmsKey.margin = JBUI.insets(6)
        txtEnglishContent.margin = JBUI.insets(6)
        txtArabicContent.margin = JBUI.insets(6)

        // Add tab to focus next text area
        txtCmsKey.enableTabTraversal(txtEnglishContent)
        txtEnglishContent.enableTabTraversal(txtArabicContent)

        // Add shift + tab to focus reverse
        txtArabicContent.enableShiftTabTraversal(txtEnglishContent)
        txtEnglishContent.enableShiftTabTraversal(txtCmsKey)

        // Use default font
        txtCmsKey.useAndroidStudioDefaultFont()
        txtEnglishContent.useAndroidStudioDefaultFont()
        txtArabicContent.useAndroidStudioDefaultFont()

        // Add border
        txtCmsKey.addBorder()
        txtEnglishContent.addBorder()
        txtArabicContent.addBorder()
    }

    private fun resetInputs() {
        txtCmsKey.text = ""
        txtEnglishContent.text = ""
        txtArabicContent.text = ""
        chkInsertAtLine.isSelected = false
        txtInsertAtLine.text = ""
    }

    private fun handleClear() {
        clearButton.addActionListener {
            resetInputs()
        }
    }

    // Ui action listeners
    private fun handleAddCmsString() {
        // button listener
        addButton.addActionListener {
            if (viewModel.addCmsString(
                    txtCmsKey.text,
                    txtEnglishContent.text,
                    txtArabicContent.text,
                    if (txtInsertAtLine.text.toString().isNotEmpty()) {
                        txtInsertAtLine.text.toInt()
                    } else 0,
                    cmbReferenceFile.selectedItem?.toString() ?: ""
                )
            ) {
                resetInputs()
            }
        }

        // insert at line
        chkInsertAtLine.addChangeListener {
            showInsertAtLineFeature(chkInsertAtLine.isSelected)
        }

        // insert at line text change listener
        txtInsertAtLine.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateTable()
            override fun removeUpdate(e: DocumentEvent?) = updateTable()
            override fun changedUpdate(e: DocumentEvent?) = updateTable()

            private fun updateTable() {
                val lineNumberText = txtInsertAtLine.text
                val newKey = txtCmsKey.text
                val newEnglishValue = txtEnglishContent.text
                val newArabicValue = txtArabicContent.text

                if (lineNumberText.isEmpty()) return

                val selectedFile = cmbReferenceFile.selectedItem as String

                if (newKey.isNotEmpty() && newEnglishValue.isNotEmpty() && newArabicValue.isNotEmpty()) {
                    if (lineNumberText.isNotEmpty()) {
                        try {
                            val lineNumber = lineNumberText.toInt()
                            if (lineNumber > 0 && lineNumber <= if (cmbReferenceFile.selectedItem == "English") {
                                    englishEntries.size + 1
                                } else {
                                    arabicEntries.size + 1
                                }
                            ) {
                                val entries = if (selectedFile == "English") {
                                    viewModel.readJsonAsList(viewModel.getEnglishJsonFile())
                                } else {
                                    viewModel.readJsonAsList(viewModel.getArabicJsonFile())
                                }
                                if (entries.isNotEmpty()) {
                                    updateTableWithSurroundingLines(tblInsertAtLine, entries, lineNumber)
                                } else {
                                    JOptionPane.showMessageDialog(frame, "The selected file is empty or not found.")
                                }
                            } else {
                                JOptionPane.showMessageDialog(frame, "Invalid line number.")
                            }
                        } catch (e: NumberFormatException) {
                            JOptionPane.showMessageDialog(frame, "Please enter a valid number.")
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "All three inputs are required.")
                }
            }
        })

        // file change listener for insert at line
        cmbReferenceFile.addActionListener {
            val entries = if (cmbReferenceFile.selectedItem?.toString().equals("English", true)) {
                viewModel.readJsonAsList(viewModel.getEnglishJsonFile())
            } else {
                viewModel.readJsonAsList(viewModel.getArabicJsonFile())
            }
            if (entries.isNotEmpty()) {
                updateTableWithSurroundingLines(tblInsertAtLine, entries, txtInsertAtLine.text.toInt())
            } else {
                JOptionPane.showMessageDialog(frame, "The selected file is empty or not found.")
            }
        }
    }

    // Populate JCombo for file selector for preview 'Insert at line'
    private fun populateInsertAtLineCombo() {
        val items = listOf("English", "Arabic").toTypedArray()
        cmbReferenceFile.model = DefaultComboBoxModel(items)
    }

    private fun showInsertAtLineFeature(show: Boolean) {
        txtInsertAtLine.isVisible = show
        jscrollInsertAtLine.isVisible = show
        tblInsertAtLine.isVisible = show
        cmbReferenceFile.isVisible = show
        txtReferenceFile.isVisible = show
        lblEnterLineNo.isVisible = show
    }

    private fun getSurroundingLines(
        entries: List<Pair<String, String>>,
        lineNumber: Int,
        newKey: String,
        newValue: String
    ): List<Array<String>>? {
        try {
            val result = mutableListOf<Array<String>>()

            // Add the two lines above (if they exist)
            if (lineNumber > 2) result.add(
                arrayOf(
                    (lineNumber - 2).toString(),
                    entries[lineNumber - 3].first,
                    entries[lineNumber - 3].second
                )
            )
            if (lineNumber > 1) result.add(
                arrayOf(
                    (lineNumber - 1).toString(),
                    entries[lineNumber - 2].first,
                    entries[lineNumber - 2].second
                )
            )

            // Add the new entry at the specified line number
            result.add(arrayOf(lineNumber.toString(), newKey, newValue))

            // Add the two lines below (if they exist)
            if (lineNumber < entries.size) result.add(
                arrayOf(
                    (lineNumber + 1).toString(),
                    entries[lineNumber - 1].first,
                    entries[lineNumber - 1].second
                )
            )
            if (lineNumber < entries.size - 1) result.add(
                arrayOf(
                    (lineNumber + 2).toString(),
                    entries[lineNumber].first,
                    entries[lineNumber].second
                )
            )

            return result
        } catch (e: Exception) {
            return null
        }
    }

    fun updateTableWithSurroundingLines(table: JTable, entries: List<Pair<String, String>>, lineNumber: Int) {
        val newValue = if (cmbReferenceFile.selectedItem?.toString().equals("English", true)) {
            txtEnglishContent.text.toString()
        } else {
            txtArabicContent.text.toString()
        }
        val surroundingLines = getSurroundingLines(entries, lineNumber, txtCmsKey.text, newValue)

        val model = DefaultTableModel(arrayOf("Line Number", "Key", "Value"), 0)
        surroundingLines?.forEach { line ->
            model.addRow(line)
        }

        table.model = model

        // Apply the custom renderer to each column for highlighting the newKey, newValue
        val renderer = CustomTableCellRenderer(lineNumber)
        for (i in 0 until table.columnCount) {
            table.columnModel.getColumn(i).cellRenderer = renderer
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun refreshTableData() {
        SwingUtilities.invokeLater {
            keyComparisonTable?.refreshTableData()
        }
    }

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
                    SwingUtilities.invokeLater {
                        keyComparisonTable?.refreshTableData()
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
                                keyComparisonTable?.refreshTableData()
                            }
                        } else {
                            JOptionPane.showMessageDialog(parentComponent, "Error adding key '$key' to CmsKeyMapper.kt.")
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
                            keyComparisonTable?.refreshTableData()
                        }
                    }
                }
            }
        }
    }

    class CustomTableCellRenderer(private val insertedLineNumber: Int) : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

            val lineNumber = table.getValueAt(row, 0).toString().toInt()
            if (lineNumber == insertedLineNumber) {
                component.background = JBColor.GREEN
            } else {
                component.background = JBColor.background()
            }
            return component
        }
    }
}

