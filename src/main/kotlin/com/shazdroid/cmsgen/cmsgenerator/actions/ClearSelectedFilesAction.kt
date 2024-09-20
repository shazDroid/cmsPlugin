package com.shazdroid.cmsgen.cmsgenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService

class ClearSelectedFilesAction : AnAction("Clear Selected Files") {
    override fun actionPerformed(e: AnActionEvent) {
        val fileService = service<FileSelectionService>()
        fileService.clearSelectedFiles()  // Clear the selected files

        Messages.showMessageDialog(
            "Plugin has been reset, now you can select files again\nNote: CmsKeyMapper, DefaultEn, DefaultArabic files are not affected by these action.",
            "Clear Files Selection",
            Messages.getInformationIcon()
        )
    }
}
