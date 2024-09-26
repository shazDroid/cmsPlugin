package com.shazdroid.cmsgen.cmsgenerator.actions

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.shazdroid.cmsgen.cmsgenerator.modifier.FileModifier
import com.shazdroid.cmsgen.cmsgenerator.modifier.JsonFileModifier
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*
import javax.swing.*
import kotlin.collections.ArrayList


class CmsInputFromCsvWindow(private val project: Project?, private val file: File?) {
    private lateinit var mainPanel: JPanel
    private lateinit var cancelButton: JButton
    private lateinit var parseButton: JButton
    private lateinit var cmbCmsKey: JComboBox<String>
    private lateinit var cmbEnglish: JComboBox<String>
    private lateinit var cmbArabic: JComboBox<String>
    private lateinit var lblArabic: JLabel
    private lateinit var lblEnglish: JLabel
    private lateinit var lblCmsKey: JLabel
    private lateinit var frame : JFrame

    private var headers: List<String> = ArrayList()
    private var csvContent: MutableList<List<String>> = mutableListOf()

    val fileModifier = FileModifier()
    val fileService = service<FileSelectionService>()
    val jsonFileModifier = JsonFileModifier()


    fun showForm() {
        frame = JFrame()
        frame.title = "CMS Input"
        frame.contentPane = mainPanel
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.pack()

        // Disable minimize behavior when showing dialogs
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowIconified(e: WindowEvent?) {
                // Ensure the window comes back to normal state if minimized
                frame.extendedState = JFrame.NORMAL
            }
        })

        frame.isVisible = true
        mainPanel.isVisible = true

        file?.let { file ->
            populateComboBoxesWithKeys(file)
        }

        cmbCmsKey.addActionListener { updateComboBoxes(cmbCmsKey) }
        cmbEnglish.addActionListener { updateComboBoxes(cmbEnglish) }
        cmbArabic.addActionListener { updateComboBoxes(cmbArabic) }

        /**
         * Parse -> ACTION
         *
         */
        parseButton.addActionListener {
            parseAndAppendToFiles()
        }

        /**
         * Cancel Button -> ACTION
         */
        cancelButton.addActionListener {
            frame.dispose()
        }
    }

    private fun populateComboBoxesWithKeys(csvFile: File) {
        try {
            BufferedReader(FileReader(csvFile)).use { br ->
                val headerLine = br.readLine() // Read the first row (headers)

                if (headerLine != null) {
                    headers = headerLine.split(",").map { it.trim() }.toMutableList()
                    csvContent.clear()

                    // Populate each comboBox with the header items
                    resetComboBoxes()

                    // Read the rest of the CSV content
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
        cmbEnglish.removeAllItems()
        cmbArabic.removeAllItems()

        headers.forEach { header ->
            cmbCmsKey.addItem(header)
            cmbEnglish.addItem(header)
            cmbArabic.addItem(header)
        }
    }

    private fun updateComboBoxes(changedComboBox: JComboBox<String>) {
        val selectedCmsKey: String? = cmbCmsKey.selectedItem as? String
        val selectedEnglish: String? = cmbEnglish.selectedItem as? String
        val selectedArabic: String? = cmbArabic.selectedItem as? String

        // Clear and reset all comboBoxes
        resetComboBoxes()

        // Remove selected items from other comboBoxes
        selectedCmsKey?.let {
            cmbEnglish.removeItem(it)
            cmbArabic.removeItem(it)
        }

        selectedEnglish?.let {
            cmbCmsKey.removeItem(it)
            cmbArabic.removeItem(it)
        }

        selectedArabic?.let {
            cmbCmsKey.removeItem(it)
            cmbEnglish.removeItem(it)
        }

        // Re-set the selected items to maintain the user's selections
        cmbCmsKey.selectedItem = selectedCmsKey
        cmbEnglish.selectedItem = selectedEnglish
        cmbArabic.selectedItem = selectedArabic
    }

    private fun parseAndAppendToFiles() {
        val selectedCmsKey: String? = cmbCmsKey.selectedItem as? String
        val selectedEnglish: String? = cmbEnglish.selectedItem as? String
        val selectedArabic: String? = cmbArabic.selectedItem as? String

        if (selectedCmsKey != null && selectedEnglish != null && selectedArabic != null) {
            // Get the indexes of the selected columns
            val cmsKeyIndex = headers.indexOf(selectedCmsKey)
            val englishIndex = headers.indexOf(selectedEnglish)
            val arabicIndex = headers.indexOf(selectedArabic)

            if (cmsKeyIndex == -1 || englishIndex == -1 || arabicIndex == -1) {
                JOptionPane.showMessageDialog(frame, "Error: Selected columns are invalid!")
                return
            }

            // Extract values from the CSV content based on the selected mappings
            val cmsKeyValues = mutableListOf<String>()
            val englishValues = mutableListOf<String>()
            val arabicValues = mutableListOf<String>()

            csvContent.forEach { row ->
                if (row.size > cmsKeyIndex && row.size > englishIndex && row.size > arabicIndex) {
                    cmsKeyValues.add(row[cmsKeyIndex])
                    englishValues.add(row[englishIndex])
                    arabicValues.add(row[arabicIndex])
                }
            }



            // Assuming you already have file paths and a Project object
            val cmsKeyFilePath = fileService.getSelectedFiles().find { it.contains("CmsKeyMapper.kt") }
            val enJsonFilePath = fileService.getSelectedFiles().find { it.contains("DefaultEn.json") }
            val arJsonFilePath = fileService.getSelectedFiles().find { it.contains("DefaultArabic.json") }


            // Append the values to the respective files using your existing functions
            cmsKeyValues.forEach { cmsKey ->
                fileModifier.appendCmsKeyToFile(cmsKeyFilePath.toString(), cmsKey, project)
            }

            englishValues.forEachIndexed { index, enContent ->
                jsonFileModifier.appendToEnglishJson(enJsonFilePath.toString(), cmsKeyValues[index], enContent, project)
            }

            arabicValues.forEachIndexed { index, arContent ->
                jsonFileModifier.appendToArabicJson(arJsonFilePath.toString(), cmsKeyValues[index], arContent, project)
            }

            // Show an informational dialog with a single "OK" button
            Messages.showInfoMessage(
                "Operation completed!",
                "Success"
            )

            frame.dispose()

        } else {
            Messages.showMessageDialog("Please select all the mappings before parsing!", "Failed", Messages.getErrorIcon())
        }
    }

}
