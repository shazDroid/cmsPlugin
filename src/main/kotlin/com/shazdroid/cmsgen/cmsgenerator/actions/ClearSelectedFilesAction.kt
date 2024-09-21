package com.shazdroid.cmsgen.cmsgenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService

class ClearSelectedFilesAction : AnAction("Reset plugin") {
    override fun actionPerformed(e: AnActionEvent) {

        // Show a confirmation dialog
        val result = Messages.showYesNoDialog(
            "Are you sure you want to reset the plugin?",
            "Confirm Clear",
            Messages.getQuestionIcon()
        )

        // Check the user's response
        if (result == Messages.YES) {
            // Logic to clear selected files
            clearSelectedFiles()
            Messages.showInfoMessage("Plugin has been reset, now you can select files again\n" +
                    "Note: CmsKeyMapper, DefaultEn, DefaultArabic files are not affected by these action.", "Clear Success")
        } else {
            // User chose not to clear the files
            Messages.showInfoMessage("Operation cancelled.", "Cancelled")
        }

//        Messages.showMessageDialog(
//            "Plugin has been reset, now you can select files again\nNote: CmsKeyMapper, DefaultEn, DefaultArabic files are not affected by these action.",
//            "Clear Files Selection",
//            Messages.getInformationIcon()
//        )
    }


    private fun clearSelectedFiles() {
        val fileService = service<FileSelectionService>()
        fileService.clearSelectedFiles()  // Clear the selected files
    }
}
