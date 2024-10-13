package com.shazdroid.cmsgen.cmsgenerator.util

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.ui.JBColor
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.NumberFormat
import javax.swing.*
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter

fun JTextArea.enableTabTraversal(nextTextArea: JTextArea) {
    this.setFocusTraversalKeysEnabled(false)

    val tabAction = "moveFocus"
    this.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("TAB"), tabAction)
    this.actionMap.put(tabAction, object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            nextTextArea.requestFocus()
        }
    })
}

fun JTextArea.enableShiftTabTraversal(previousTextArea: JTextArea) {
    val shiftTabAction = "moveFocusBackward"
    this.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("shift TAB"), shiftTabAction)
    this.actionMap.put(shiftTabAction, object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            previousTextArea.requestFocus()
        }
    })
}


fun JTextArea.useAndroidStudioDefaultFont() {
    val font = EditorColorsManager.getInstance().globalScheme.getFont(EditorFontType.PLAIN)

    this.font = font
}


fun JTextArea.addBorder(thickness: Int = 1, color: JBColor = JBColor.GRAY) {
    this.border = BorderFactory.createLineBorder(color, thickness)
}


fun JFormattedTextField.restrictToIntegerInput() {
    val integerFormat = NumberFormat.getIntegerInstance().apply {
        isGroupingUsed = false
    }

    val numberFormatter = NumberFormatter(integerFormat).apply {
        valueClass = Int::class.java
        allowsInvalid = false
        minimum = Int.MIN_VALUE
        maximum = Int.MAX_VALUE
    }

    formatterFactory = DefaultFormatterFactory(numberFormatter)

    value = 0
}

fun String.icon(): String {
    return "/icons/$this"
}

fun JLabel.onClick(action: () -> Unit) {
    this.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) {
            action()
        }
    })
}