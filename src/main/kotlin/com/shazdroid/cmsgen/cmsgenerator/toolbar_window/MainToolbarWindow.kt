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
        // Adjust the preferred size dynamically based on the content
        val preferredWidth = panel.preferredSize.width
        val minimumWidth = 300 // You can set a minimum width if needed

        // Set the preferred size to make sure the panel grows with the content
        panel.preferredSize = panel.preferredSize.apply {
            width = preferredWidth.coerceAtLeast(minimumWidth)
        }
        panel.revalidate()
        panel.repaint()
    }
}
