package com.shazdroid.cmsgen.cmsgenerator.cms_media_keys

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFileManager
import java.awt.Component
import javax.swing.AbstractCellEditor
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.table.TableCellEditor

class ButtonEditor(private val project: Project, private val tableData: List<TableRowData>) : AbstractCellEditor(),
    TableCellEditor {
    private val button = JButton()
    private var currentRow = -1

    init {
        button.addActionListener {
            if (currentRow >= 0) {
                val rowData = tableData[currentRow]
                if (rowData.type == "Missing Asset") {
                    showImage(rowData.path)
                } else if (rowData.type == "Unused Key") {
                }
            }
        }
    }

    override fun getTableCellEditorComponent(
        table: JTable?, value: Any?, isSelected: Boolean,
        row: Int, column: Int
    ): Component {
        currentRow = row
        button.text = value?.toString() ?: ""
        return button
    }

    override fun getCellEditorValue(): Any {
        return button.text
    }

    private fun showImage(imagePath: String) {
        val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://$imagePath")
        if (virtualFile != null) {
            OpenFileDescriptor(project, virtualFile).navigate(true)
        } else {
            Messages.showErrorDialog(project, "Image file not found: $imagePath", "Error")
        }
    }

}
