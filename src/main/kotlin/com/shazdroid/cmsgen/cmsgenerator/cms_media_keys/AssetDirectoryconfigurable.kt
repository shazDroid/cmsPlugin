package com.shazdroid.cmsgen.cmsgenerator.cms_media_keys

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent

class AssetsDirectoryConfigurable(private val project: Project) : Configurable {

    private var assetsDirectoryField: TextFieldWithBrowseButton? = null

    override fun createComponent(): JComponent? {
        assetsDirectoryField = TextFieldWithBrowseButton()
        assetsDirectoryField?.addBrowseFolderListener(
            "Select Assets Directory",
            null,
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
        val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Assets Directory:", assetsDirectoryField!!)
            .panel
        return panel
    }

    override fun isModified(): Boolean {
        val settings = AssetsDirectorySettings.getInstance(project)
        return assetsDirectoryField?.text != settings.assetsDirectoryPath
    }

    override fun apply() {
        val settings = AssetsDirectorySettings.getInstance(project)
        settings.assetsDirectoryPath = assetsDirectoryField?.text ?: ""
    }

    override fun reset() {
        val settings = AssetsDirectorySettings.getInstance(project)
        assetsDirectoryField?.text = settings.assetsDirectoryPath
    }

    override fun getDisplayName(): String {
        return "Assets Directory"
    }
}
