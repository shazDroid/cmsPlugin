package com.shazdroid.cmsgen.cmsgenerator.actions


import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class InfoAction : AnAction("Plugin Info") {
    override fun actionPerformed(e: AnActionEvent) {
        // Display developer information in a message dialog
        Messages.showMessageDialog(
            "Developer: Shahbaz Ansari" +
                    "\nEmail: shahbazansari52@gmail.com" +
                    "\nVersion: 7.0",
            "Developer Information",
            Messages.getInformationIcon()
        )
    }
}
