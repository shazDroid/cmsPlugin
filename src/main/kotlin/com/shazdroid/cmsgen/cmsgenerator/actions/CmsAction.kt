package com.shazdroid.cmsgen.cmsgenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService

class CmsAction : AnAction("CMS generator") {
    override fun actionPerformed(event: AnActionEvent) {
        val project: Project? = event.project
        // check if files are choosen
        val fileSelectionService = service<FileSelectionService>()

        if (fileSelectionService.getSelectedFiles().isNotEmpty()){
            val cmsInputActionWindow = CmsInputActionWindow(project)
            cmsInputActionWindow?.showForm()
            return
        }

        // Show the custom dialog
        if (project != null) {
            val dialog = FileChooserDialog(project)
            dialog.show()
        }
    }
}