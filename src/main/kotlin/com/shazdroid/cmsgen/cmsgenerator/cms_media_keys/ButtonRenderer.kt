package com.shazdroid.cmsgen.cmsgenerator.cms_media_keys

import com.intellij.icons.AllIcons
import java.awt.Component
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

class ButtonRenderer : JButton(), TableCellRenderer {
    init {
        isOpaque = true
        icon = AllIcons.Actions.Preview
    }

    override fun getTableCellRendererComponent(
        table: JTable?, value: Any?, isSelected: Boolean,
        hasFocus: Boolean, row: Int, column: Int
    ): Component {
        return this
    }
}
