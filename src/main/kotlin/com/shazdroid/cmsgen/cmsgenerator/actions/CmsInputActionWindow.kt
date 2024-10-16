package com.shazdroid.cmsgen.cmsgenerator.actions

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.shazdroid.cmsgen.cmsgenerator.modifier.FileModifier
import com.shazdroid.cmsgen.cmsgenerator.modifier.JsonFileModifier
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService
import java.awt.Color
import java.awt.Cursor
import java.awt.Desktop
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.URI
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
    private lateinit var developerInfo: JLabel

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

            //var isSuccess = false'
            var fileOperations: FileModifier.FileOperationResult? = null

            if (cmsKey.isNotEmpty() && engContent.isNotEmpty() && arContent.isNotEmpty()) {
                fileService.getSelectedFiles().first()
                fileService.getSelectedFiles().forEachIndexed { index, item ->
                    if (item.contains("CmsKeyMapper.kt")) {
                        fileOperations = fileModifier.appendCmsKeyToFile(item, cmsKey.trim(), project)
                    }
                }

                when (fileOperations) {
                    FileModifier.FileOperationResult.SUCCESS -> {
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

                        Messages.showMessageDialog(
                            frame,
                            "CMS Key Added Successfully",
                            "Success",
                            Messages.getInformationIcon()
                        )
                        frame.dispose()
                    }

                    FileModifier.FileOperationResult.FILE_NOT_FOUND -> {

                    }

                    FileModifier.FileOperationResult.DUPLICATE_KEY -> {

                    }

                    FileModifier.FileOperationResult.COMPANION_OBJECT_NOT_FOUND -> {

                    }

                    FileModifier.FileOperationResult.WRITE_ERROR -> {

                    }

                    null -> {

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
         * Find and Replace - ACTION
         *
         */
        findAndReplaceButton.addActionListener {
            Messages.showMessageDialog("Under development...", "Coming soon", Messages.getInformationIcon())
        }


        /**
         * Cancel Button -> ACTION
         */
        cancelButton.addActionListener {
            frame.dispose()
        }

        /**
         * Developer Info -> ACTION
         */
        developerInfo.addMouseListener(object : MouseAdapter(){

            override fun mouseEntered(e: MouseEvent?) {
                // Change color when mouse enters
                developerInfo.foreground = Color.BLUE
                // Change cursor to hand cursor
                developerInfo.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            }

            override fun mouseExited(e: MouseEvent?) {
                // Reset to original color when mouse exits
                developerInfo.foreground = Color.WHITE
                // Reset cursor to default
                developerInfo.cursor = Cursor.getDefaultCursor()
            }

            override fun mouseClicked(e: MouseEvent?) {
                try {
                    // Open the URL in the system's default browser
                    val url = URI("https://www.linkedin.com/in/shahbaz-ansari-449014b7/")
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(url)
                    } else {
                        JOptionPane.showMessageDialog(
                            frame,
                            "Desktop not supported.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        })
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
