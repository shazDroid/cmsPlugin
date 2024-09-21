package com.shazdroid.cmsgen.cmsgenerator.actions


import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService
import java.awt.GridLayout
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class FileChooserDialog(private val project: Project) : DialogWrapper(project) {

    private val file1Label = JLabel("File 1: Not selected")
    private val file2Label = JLabel("File 2: Not selected")
    private val file3Label = JLabel("File 3: Not selected")

    private var selectedFile1: File? = null
    private var selectedFile2: File? = null
    private var selectedFile3: File? = null

    init {
        // Set dialog title
        title = "Choose Files"

        // Initialize the dialog panel with file chooser buttons and labels
        init()
    }

    override fun createCenterPanel(): JComponent? {
        val panel = JPanel(GridLayout(4, 2))

        // Add file 1 button and label
        val file1Button = JButton("Choose DefaultEn.json")
        file1Button.addActionListener { chooseFile(1) }
        panel.add(file1Button)
        panel.add(file1Label)

        // Add file 2 button and label
        val file2Button = JButton("Choose DefaultArabic.json")
        file2Button.addActionListener { chooseFile(2) }
        panel.add(file2Button)
        panel.add(file2Label)

        // Add file 3 button and label
        val file3Button = JButton("Choose CmsKeyMapper file")
        file3Button.addActionListener { chooseFile(3) }
        panel.add(file3Button)
        panel.add(file3Label)

        return panel
    }

    // Function to choose a file based on button number
    private fun chooseFile(fileNumber: Int) {
        val fileChooser = JFileChooser(project.basePath)

        when(fileNumber){
            1 -> fileChooser.dialogTitle = "Choose DefaultEn.json"
            2 -> fileChooser.dialogTitle = "Choose DefaultArabic.json file"
            3 -> fileChooser.dialogTitle = "Choose CmsKeyMapper file"
        }

        // Optionally, restrict file types (e.g., to specific extensions)
        fileChooser.fileFilter = FileNameExtensionFilter("Text Files", "json","kt")

        // Open file chooser dialog
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile
            when (fileNumber) {
                1 -> {
                    selectedFile1 = selectedFile
                    file1Label.text = "Eng : ${selectedFile.absolutePath}"
                }
                2 -> {
                    selectedFile2 = selectedFile
                    file2Label.text = "Arabic: ${selectedFile.absolutePath}"
                }
                3 -> {
                    selectedFile3 = selectedFile
                    file3Label.text = "CmsMapperKey : ${selectedFile.absolutePath}"
                }
            }
        }
    }

    private fun storeSelectedFiles(filePaths: List<String>) {
        val fileSelectionService = service<FileSelectionService>()
        fileSelectionService.storeSelectedFiles(filePaths)
    }

    // Handle OK button click (collect the selected files)
    override fun doOKAction() {
        if (selectedFile1 != null && selectedFile2 != null && selectedFile3 != null) {

            // Store the selected files' absolute paths
            storeSelectedFiles(listOf(selectedFile1!!.absolutePath, selectedFile2!!.absolutePath, selectedFile3!!.absolutePath))

            Messages.showMessageDialog(
                "File selection completed",
                "Files Selected",
                Messages.getInformationIcon()
            )

            // Close the dialog after files are stored
            super.doOKAction()

        } else {
            Messages.showMessageDialog(
                "Please select DefaultEn.json, DefaultArabic.json, CmsMapperKey.kt to proceed",
                "Error",
                Messages.getErrorIcon()
            )
        }
    }
}
