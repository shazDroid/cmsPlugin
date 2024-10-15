package com.shazdroid.cmsgen.cmsgenerator.cms_media_keys

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class AssetsComparisonToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val assetsComparisonToolWindow = AssetsComparisonToolWindow(project)
        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(assetsComparisonToolWindow, null, false)
        contentManager.addContent(content)
    }
}