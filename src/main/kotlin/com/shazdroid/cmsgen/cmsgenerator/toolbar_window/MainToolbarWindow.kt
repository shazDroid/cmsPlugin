package com.shazdroid.cmsgen.cmsgenerator.toolbar_window


import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.shazdroid.cmsgen.cmsgenerator.window.CmsMainWindow
import javax.swing.JPanel

class MainToolbarWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val cmsMainWindow = CmsMainWindow(project)
        val mainWindowPanel = cmsMainWindow.mainPanel
        cmsMainWindow.showForm()

        adjustPanelSize(mainWindowPanel)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(mainWindowPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun adjustPanelSize(panel: JPanel) {
        val preferredWidth = panel.preferredSize.width
        val minimumWidth = 300

        panel.preferredSize = panel.preferredSize.apply {
            width = preferredWidth.coerceAtLeast(minimumWidth)
        }
        panel.revalidate()
        panel.repaint()
    }
}
