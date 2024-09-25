package com.shazdroid.cmsgen.cmsgenerator.actions

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.shazdroid.cmsgen.cmsgenerator.modifier.FileModifier
import com.shazdroid.cmsgen.cmsgenerator.modifier.JsonFileModifier
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter


class CmsInputActionWindow(private val project: Project?) {
    private lateinit var mainPanel: JPanel
    private lateinit var txtEnglishContent: JTextArea
    private lateinit var txtArabicContent: JTextArea
    private lateinit var addButton: JButton
    private lateinit var findAndReplaceButton: JButton
    private lateinit var uploadCsvFileButton: JButton
    private lateinit var cancelButton: JButton
    private lateinit var txtCmsKey: JTextField
    private lateinit var addMultipleCheckbox: JCheckBox

    val fileModifier = FileModifier()
    val fileService = service<FileSelectionService>()
    val jsonFileModifier = JsonFileModifier()

    fun showForm() {
        val frame = JFrame()
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

        /**
         * Add Button - ACTION
         * Add contents to files
         */
        addButton.addActionListener {
            // Ensure the frame stays on top
            frame.isAlwaysOnTop = true

            val cmsKey = txtCmsKey.text ?: ""
            val engContent = txtEnglishContent.text ?: ""
            val arContent = txtArabicContent.text ?: ""

            var isSuccess = false

            if (cmsKey.isNotEmpty() && engContent.isNotEmpty() && arContent.isNotEmpty()) {
                fileService.getSelectedFiles().first()
                fileService.getSelectedFiles().forEachIndexed { index, item ->
                    if (item.contains("CmsKeyMapper.kt")) {
                        isSuccess = fileModifier.appendCmsKeyToFile(item, cmsKey.trim(), project)
                    }
                }

                if (isSuccess) {
                    fileService.getSelectedFiles().forEachIndexed { _, item ->
                        if (item.contains("DefaultEn.json")) {
                            jsonFileModifier.appendToEnglishJson(
                                item,
                                cmsKey,
                                engContent.trim(),
                                project
                            )
                        }

                        if (item.contains("DefaultArabic.json")) {
                            jsonFileModifier.appendToArabicJson(
                                item,
                                cmsKey,
                                arContent.trim(),
                                project
                            )
                        }
                    }
                }

                if (isSuccess) {
                    if (addMultipleCheckbox.isSelected) {
                        Messages.showMessageDialog(
                            frame,
                            "CMS Key Added Successfully",
                            "Success",
                            Messages.getInformationIcon()
                        )
                        clearFields()
                    } else {
                        Messages.showMessageDialog(
                            frame,
                            "CMS Key Added Successfully",
                            "Success",
                            Messages.getInformationIcon()
                        )
                        frame.dispose()
                    }
                }

                // Restore the normal behavior after the dialog
                frame.isAlwaysOnTop = false

            } else {
                Messages.showMessageDialog(frame, "Please fill in all three inputs.", "Error", Messages.getErrorIcon())
            }
        }


        /**
         * Upload csv - ACTION
         *
         */
        uploadCsvFileButton.addActionListener {
            openCsvFileChooser()
        }


        /**
         * Cancel Button -> ACTION
         */
        cancelButton.addActionListener {
            frame.dispose()
        }
    }

    fun openCsvFileChooser() {
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
            val uploadCsvForm = CmsInputFromCsvWindow(project, fileChooser.selectedFile)
            uploadCsvForm.showForm()
        } else {
            null // User canceled the file selection
        }
    }

    private fun clearFields() {
        txtCmsKey.text = ""
        txtEnglishContent.text = ""
        txtArabicContent.text = ""
    }
}
