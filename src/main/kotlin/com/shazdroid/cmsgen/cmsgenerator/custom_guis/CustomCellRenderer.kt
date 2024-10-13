package com.shazdroid.cmsgen.cmsgenerator.custom_guis

import java.awt.Color
import java.awt.Component
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer


class CustomCellRenderer(private val insertedLineNumber: Int) : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable, value: Any?, isSelected: Boolean,
        hasFocus: Boolean, row: Int, column: Int
    ): Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        val lineNumberValue = table.model.getValueAt(row, 0) as Int
        if (lineNumberValue == insertedLineNumber) {
            component.background =  Color(48, 164, 75)
        } else {
            component.background = if (isSelected) table.selectionBackground else table.background
        }

        return component
    }
}