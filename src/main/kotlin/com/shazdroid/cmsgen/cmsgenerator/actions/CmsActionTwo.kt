package com.shazdroid.cmsgen.cmsgenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService
import com.shazdroid.cmsgen.cmsgenerator.window.CmsMainWindow

class CmsActionTwo : AnAction("CMS generator 2.0") {
    override fun actionPerformed(event: AnActionEvent) {
        val project: Project? = event.project
        // check if files are choosen
        val fileSelectionService = service<FileSelectionService>()

        if (fileSelectionService.getSelectedFiles().isNotEmpty()){
            /* val inputDialog = InputContentDialog(project)
             inputDialog.show()
             return*/

            // val cmsInputActionWindow = CmsInputActionWindow(project)
            val cmsMainWindow = project?.let { CmsMainWindow(it) }
            cmsMainWindow?.showForm()
            return
        }

        // Show the custom dialog
        if (project != null) {
            val dialog = FileChooserDialog(project)
            dialog.show()
        }
    }
}