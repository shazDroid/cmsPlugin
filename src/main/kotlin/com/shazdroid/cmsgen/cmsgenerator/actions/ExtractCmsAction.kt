package com.shazdroid.cmsgen.cmsgenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.shazdroid.cmsgen.cmsgenerator.modifier.FileModifier
import com.shazdroid.cmsgen.cmsgenerator.modifier.JsonFileModifier
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService
import javax.swing.*

class ExtractCmsAction : AnAction() {

    val fileModifier = FileModifier()
    val fileService = service<FileSelectionService>()
    val jsonFileModifier = JsonFileModifier()

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val editor: Editor? = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR)

        // Check if there is selected text
        if (editor != null) {
            val selectedText = editor.selectionModel.selectedText
            if (selectedText != null && selectedText.isNotEmpty()) {
                // Show the custom window with pre-filled content
                showExtractToCmsDialog(selectedText, project, editor)
            } else {
                Messages.showMessageDialog(
                    project,
                    "No text selected. Please select a hardcoded string.",
                    "Error",
                    Messages.getErrorIcon()
                )
            }
        }
    }

    private fun showExtractToCmsDialog(selectedText: String, project: Project?, editor: Editor) {
        // Create the input fields
        val cmsKeyField = JTextField(20)
        val englishTextField = JTextField(20)
        val arabicTextField = JTextField(20)

        // Prefill the fields
        cmsKeyField.text = "AutoGeneratedKey"
        englishTextField.text = selectedText
        arabicTextField.text = ""

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JLabel("CMS Key:"))
        panel.add(cmsKeyField)
        panel.add(JLabel("English Text:"))
        panel.add(englishTextField)
        panel.add(JLabel("Arabic Text:"))
        panel.add(arabicTextField)

        val result = JOptionPane.showConfirmDialog(
            null,
            panel,
            "Extract to CMS String",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        )

        if (result == JOptionPane.OK_OPTION) {
            val cmsKey = cmsKeyField.text
            val englishText = englishTextField.text
            val arabicText = arabicTextField.text

            val cmsKeyFilePath = fileService.getSelectedFiles().find { it.contains("CmsKeyMapper.kt") }
            val enJsonFilePath = fileService.getSelectedFiles().find { it.contains("DefaultEn.json") }
            val arJsonFilePath = fileService.getSelectedFiles().find { it.contains("DefaultArabic.json") }

            // Append the values to the relevant files
            fileModifier.appendCmsKeyToFile(cmsKeyFilePath ?: "", cmsKey.replace("\"", ""), project)

            jsonFileModifier.appendToEnglishJson(
                enJsonFilePath ?: "",
                cmsKey,
                englishText,
                project
            )

            jsonFileModifier.appendToArabicJson(
                arJsonFilePath ?: "",
                cmsKey,
                arabicText,
                project
            )

            WriteCommandAction.runWriteCommandAction(project) {
                // Replace the selected text with the getCmsString call
                val document = editor.document
                val selectionModel = editor.selectionModel
                val start = selectionModel.selectionStart
                val end = selectionModel.selectionEnd

                // Replace the selected text with getCmsString(CmsKeyMapper.CMS_KEY)
                val replacementText = "getCmsString(CmsKeyMapper.${fileModifier.camelToSnakeCase(cmsKey)})"
                document.replaceString(start, end, replacementText)

                // Deselect the text after replacement
                selectionModel.removeSelection()
            }

            Messages.showInfoMessage("CMS Key and contents added successfully!", "Success")
        }
    }
}
