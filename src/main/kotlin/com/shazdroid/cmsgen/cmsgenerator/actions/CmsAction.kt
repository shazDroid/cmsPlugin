package com.shazdroid.cmsgen.cmsgenerator.actions

import InputContentDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService

class CmsAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project: Project? = event.project
        // check if files are choosen
        val fileSelectionService = service<FileSelectionService>()

        if (fileSelectionService.getSelectedFiles().isNotEmpty()){
            val inputDialog = InputContentDialog()
            inputDialog.show()
            return
        }

        // Show the custom dialog
        if (project != null) {
            val dialog = FileChooserDialog(project)
            dialog.show()
        }
    }
}