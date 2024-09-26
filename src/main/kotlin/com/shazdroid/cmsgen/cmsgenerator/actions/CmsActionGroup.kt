package com.shazdroid.cmsgen.cmsgenerator.actions


import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class CmsActionGroup : ActionGroup("CMS Generator",true) {

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        // Return the actions that should appear in the dropdown
        return arrayOf(
            CmsAction(),           // Cms Generator action
            ClearSelectedFilesAction() , // Clear Selected Files action
            InfoAction() // Developer info action
        )
    }
}
