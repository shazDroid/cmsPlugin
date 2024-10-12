package com.shazdroid.cmsgen.cmsgenerator.toolbar_window


import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.shazdroid.cmsgen.cmsgenerator.window.CmsMainWindow
import com.shazdroid.cmsgen.cmsgenerator.window.JsonFormatter
import javax.swing.JPanel

class JsonFormatterMainToolbarWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val jsonFormatter = JsonFormatter()
        val mainWindowPanel = jsonFormatter.mainPanel

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
