package com.shazdroid.cmsgen.cmsgenerator.window

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.shazdroid.cmsgen.cmsgenerator.actions.FileChooserDialog
import com.shazdroid.cmsgen.cmsgenerator.keycomparison.KeyComparisonTable
import com.shazdroid.cmsgen.cmsgenerator.modifier.FileChangeListener
import com.shazdroid.cmsgen.cmsgenerator.modifier.FileModifier
import com.shazdroid.cmsgen.cmsgenerator.modifier.JsonFileModifier
import com.shazdroid.cmsgen.cmsgenerator.operations.Operations
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService
import com.shazdroid.cmsgen.cmsgenerator.util.*
import com.shazdroid.cmsgen.cmsgenerator.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Component
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.filechooser.FileNameExtensionFilter
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
    lateinit var fileNotSelectedLabel: JLabel
    lateinit var goToSettingsButton: JButton

    // Compare components
    lateinit var comparePanel: JPanel
    lateinit var compareJScrollPane: JScrollPane
    lateinit var comparisonTable: JTable
    lateinit var txtSearchField: JTextField
    lateinit var lblSearch: JLabel
    lateinit var refreshLabel: JLabel
    lateinit var progressBar: JProgressBar


    // Bulk parsing
    lateinit var chooseFileButton: JButton
    lateinit var csvTable: JTable
    lateinit var keySelectionPanel: JPanel
    lateinit var cmbCmsKey: JComboBox<String>
    lateinit var cmbEnglishKey: JComboBox<String>
    lateinit var cmbArabicKey: JComboBox<String>
    private var headers: List<String> = ArrayList()
    private var csvContent: MutableList<List<String>> = mutableListOf()
    lateinit var sepOne: JSeparator
    lateinit var sepTwo: JSeparator
    lateinit var parseButton: JButton
    val originalCmsKeys = mutableListOf<String>()
    val originalEnglishKeys = mutableListOf<String>()
    val originalArabicKeys = mutableListOf<String>()
    lateinit var bulkOperations: Operations.BulkAddOperations
    lateinit var csvScrollPane: JScrollPane
    lateinit var developerPanel: JPanel
    lateinit var selectedCmsKeyFilePathLabel: JLabel
    lateinit var selectCmsMediaKeyButton: JButton

    // Settings
    lateinit var selectedFilesList: JList<String>
    lateinit var resetFileSelection: JButton
    lateinit var addFilesButton: JButton
    val selectedFileModels = DefaultListModel<String>()
    var isFilesAlreadyAddedInList = false

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

    fun showForm() {
        checkFileSelections()
        generateFrame()
        initUi()
        handleAddCmsString()
        addIconsToTab()
        handleClear()
        tabOperations()
        registerJsonFileChangeListener()
    }

    private fun registerJsonFileChangeListener() {
        val fileManager = VirtualFileManager.getInstance()
        val listener = FileChangeListener(project) {
        }
        fileManager.addVirtualFileListener(listener)
    }


    private fun performFileSelection() {
        val fileChooserDialog = FileChooserDialog(project) {
            when (it) {
                FileModifier.FileOperationResult.SUCCESS -> {
                    keyComparisonTable?.refreshTableData()
                    keyComparisonTable?.refreshTableData()
                }

                FileModifier.FileOperationResult.FILE_NOT_FOUND -> {
                    keyComparisonTable?.refreshTableData()
                }

                FileModifier.FileOperationResult.DUPLICATE_KEY -> {}
                FileModifier.FileOperationResult.COMPANION_OBJECT_NOT_FOUND -> {}
                FileModifier.FileOperationResult.WRITE_ERROR -> {}
            }
        }
        fileChooserDialog.show()
    }

    private fun clearFileSelection() {
        val result = Messages.showYesNoDialog(
            "Are you sure you want to reset the plugin?",
            "Confirm Clear",
            Messages.getQuestionIcon()
        )


        if (result == Messages.YES) {
            fileService.clearSelectedFiles()
            Messages.showInfoMessage(
                "Plugin has been reset, now you can select files again\n" +
                        "Note: CmsKeyMapper, DefaultEn, DefaultArabic files are not affected by these action.",
                "Clear Success"
            )
            isFilesAlreadyAddedInList = false
            keyComparisonTable?.refreshTableData()
        } else {
            Messages.showInfoMessage("Operation cancelled.", "Cancelled")
        }
    }

    private fun checkFileSelections() {
        if (fileService.getSelectedFiles().isEmpty()) {
            fileNotSelectedLabel.isVisible = true
            goToSettingsButton.isVisible = true
        } else {
            fileNotSelectedLabel.isVisible = false
            goToSettingsButton.isVisible = false
        }
    }


    private fun compareOperations() {
        refreshLabel.icon = IconLoader.getIcon("refresh.svg".icon(), javaClass)
        progressBar.isVisible = false
        progressBar.isStringPainted = true
        progressBar.string = "Loading data..."

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

        keyComparisonTable = KeyComparisonTable(
            table = comparisonTable,
            enFile = enFile,
            arFile = arFile,
            viewModel = viewModel,
            compareOperations = compareOperations,
            project = project,
            progressBar = progressBar,
            searchTextField = txtSearchField
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
        handleFileSelectionUi()
        showKeySelectionForBulkParse()
        // initCmsMediaKeyOperations()

        goToSettingsButton.addActionListener {
            mainTabLayout.selectedIndex = 3
        }


        resetFileSelection.addActionListener {
            clearFileSelection()
            handleFileSelectionUi()
        }


        addFilesButton.addActionListener {
            performFileSelection()
            handleFileSelectionUi()
        }

        // file chooser for csv
        chooseFileButton.addActionListener {
            bulkOperations = operations.value.BulkAddOperations()
            openCsvFileChooser()
        }


        populateFilesListInSettings()
    }

    private fun handleFileSelectionUi() {
        if (fileService.getSelectedFiles().isEmpty()) {
            addFilesButton.isEnabled = true
            selectedFileModels.removeAllElements()
            selectedFileModels.addElement("No files selected !")
            isFilesAlreadyAddedInList = false
        } else {
            addFilesButton.isEnabled = false
            populateFilesListInSettings()
            if (selectedFileModels.contains("No files selected!")) {
                selectedFileModels.removeElement("No files selected!")
            }
        }
    }

    private fun populateFilesListInSettings() {

        selectedFilesList.border = BorderFactory.createLineBorder(JBColor.GRAY, 1)
        if (isFilesAlreadyAddedInList.not()) {
            fileService.getSelectedFiles().forEach { item ->
                if (item.contains("DefaultEn.json")) {
                    selectedFileModels.addElement("English File : $item")
                }

                if (item.contains("DefaultArabic.json")) {
                    selectedFileModels.addElement("Arabic File : $item")
                }

                if (item.contains("CmsKeyMapper.kt")) {
                    selectedFileModels.addElement("CmsKeyMapper File : $item")
                }
            }
            isFilesAlreadyAddedInList = true
            selectedFilesList.model = selectedFileModels
        }
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


        txtCmsKey.enableTabTraversal(txtEnglishContent)
        txtEnglishContent.enableTabTraversal(txtArabicContent)


        txtArabicContent.enableShiftTabTraversal(txtEnglishContent)
        txtEnglishContent.enableShiftTabTraversal(txtCmsKey)


        txtCmsKey.useAndroidStudioDefaultFont()
        txtEnglishContent.useAndroidStudioDefaultFont()
        txtArabicContent.useAndroidStudioDefaultFont()


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


        chkInsertAtLine.addChangeListener {
            showInsertAtLineFeature(chkInsertAtLine.isSelected)
        }


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

            result.add(arrayOf(lineNumber.toString(), newKey, newValue))

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

        val renderer = CustomTableCellRenderer(lineNumber)
        for (i in 0 until table.columnCount) {
            table.columnModel.getColumn(i).cellRenderer = renderer
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

    private fun showKeySelectionForBulkParse(show: Boolean = false) {
        if (show) {
            keySelectionPanel.isVisible = true
            sepOne.isVisible = true
            sepTwo.isVisible = true
        } else {
            keySelectionPanel.isVisible = false
            sepOne.isVisible = false
            sepTwo.isVisible = false
        }
    }

    private fun initBulkParseUi(selectedFile: File) {
        showKeySelectionForBulkParse(true)
        val tableModel = operations.value.BulkAddOperations().readCsvAsTableModel(selectedFile.path)
        val csvData = bulkOperations.parseCsvFileToTable(selectedFile, csvTable)

        selectedFile?.let { file ->
            populateComboBoxesWithKeys(file)
        }


        parseButton.addActionListener {
            val cmsKeyColumnIndex = cmbCmsKey.selectedIndex
            val englishColumnIndex = cmbEnglishKey.selectedIndex
            val arabicColumnIndex = cmbArabicKey.selectedIndex

            bulkOperations.processBulkKeysFromCsv(
                csvData.drop(1),
                cmsKeyColumnIndex,
                englishColumnIndex,
                arabicColumnIndex,
                viewModel
            )
        }
    }



    private fun populateComboBoxesWithKeys(csvFile: File) {
        try {
            BufferedReader(FileReader(csvFile)).use { br ->
                val headerLine = br.readLine()

                if (headerLine != null) {
                    headers = headerLine.split(",").map { it.trim() }.toMutableList()
                    csvContent.clear()
                    originalCmsKeys.addAll(headers)
                    originalEnglishKeys.addAll(headers)
                    originalArabicKeys.addAll(headers)

                    resetComboBoxes()

                    br.lines().forEach { line ->
                        csvContent.add(line.split(",").map { it.trim() })
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun resetComboBoxes() {
        cmbCmsKey.removeAllItems()
        cmbEnglishKey.removeAllItems()
        cmbArabicKey.removeAllItems()

        headers.forEach { header ->
            cmbCmsKey.addItem(header)
            cmbEnglishKey.addItem(header)
            cmbArabicKey.addItem(header)
        }
    }

    private fun openCsvFileChooser() {
        // Create a file chooser
        val fileChooser = JFileChooser()

        // Set the file filter to allow only CSV files
        val csvFilter = FileNameExtensionFilter("CSV Files", "csv")
        fileChooser.fileFilter = csvFilter

        // Set dialog title
        fileChooser.dialogTitle = "Select a CSV File"

        // Show the dialog; the return value indicates whether the user selected a file
        val result = fileChooser.showOpenDialog(null)

        // If the user approved the file selection, return the selected file
        if (result == JFileChooser.APPROVE_OPTION) {
            initBulkParseUi(fileChooser.selectedFile)
        } else {
            null // User canceled the file selection
        }
    }

    fun addComboBoxSelectionListeners(
        cmsComboBox: JComboBox<String>,
        englishComboBox: JComboBox<String>,
        arabicComboBox: JComboBox<String>,
        originalCmsKeys: List<String>,
        originalEnglishKeys: List<String>,
        originalArabicKeys: List<String>
    ) {
        cmsComboBox.addActionListener {
            val selectedKey = cmsComboBox.selectedItem as? String ?: return@addActionListener
            updateComboBoxesAfterSelection(
                selectedKey,
                cmsComboBox,
                englishComboBox,
                arabicComboBox,
                originalCmsKeys,
                originalEnglishKeys,
                originalArabicKeys
            )
        }

        englishComboBox.addActionListener {
            val selectedKey = englishComboBox.selectedItem as? String ?: return@addActionListener
            updateComboBoxesAfterSelection(
                selectedKey,
                englishComboBox,
                cmsComboBox,
                arabicComboBox,
                originalCmsKeys,
                originalEnglishKeys,
                originalArabicKeys
            )
        }

        arabicComboBox.addActionListener {
            val selectedKey = arabicComboBox.selectedItem as? String ?: return@addActionListener
            updateComboBoxesAfterSelection(
                selectedKey,
                arabicComboBox,
                cmsComboBox,
                englishComboBox,
                originalCmsKeys,
                originalEnglishKeys,
                originalArabicKeys
            )
        }
    }


    private fun updateComboBoxesAfterSelection(
        selectedKey: String,
        currentComboBox: JComboBox<String>,
        comboBox1: JComboBox<String>,
        comboBox2: JComboBox<String>,
        originalCmsKeys: List<String>,
        originalEnglishKeys: List<String>,
        originalArabicKeys: List<String>
    ) {
        // Update CMS ComboBox
        if (currentComboBox !== comboBox1) {
            updateComboBox(comboBox1, selectedKey, originalEnglishKeys)
        }
        if (currentComboBox !== comboBox2) {
            updateComboBox(comboBox2, selectedKey, originalArabicKeys)
        }
    }

    private fun updateComboBox(comboBox: JComboBox<String>, selectedKey: String, originalKeys: List<String>) {
        // Restore original keys, except the selected key
        comboBox.removeAllItems()
        originalKeys.filter { it != selectedKey }.forEach { comboBox.addItem(it) }
    }
}

