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
    // Disable the default tab behavior
    this.setFocusTraversalKeysEnabled(false)

    // Add a custom key binding for the Tab key
    val tabAction = "moveFocus"
    this.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("TAB"), tabAction)
    this.actionMap.put(tabAction, object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            nextTextArea.requestFocus() // Move focus to the next text area
        }
    })
}

fun JTextArea.enableShiftTabTraversal(previousTextArea: JTextArea) {
    // Add a custom key binding for Shift+Tab to move to the previous text area
    val shiftTabAction = "moveFocusBackward"
    this.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("shift TAB"), shiftTabAction)
    this.actionMap.put(shiftTabAction, object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            previousTextArea.requestFocus() // Move focus to the previous text area
        }
    })
}


fun JTextArea.useAndroidStudioDefaultFont() {
    // Retrieve the default editor font from Android Studio settings
    val font = EditorColorsManager.getInstance().globalScheme.getFont(EditorFontType.PLAIN)

    // Set the font to the JTextArea
    this.font = font
}


fun JTextArea.addBorder(thickness: Int = 1, color: JBColor = JBColor.GRAY) {
    this.border = BorderFactory.createLineBorder(color, thickness)
}


fun JFormattedTextField.restrictToIntegerInput() {
    // Create a NumberFormat instance for integers without grouping (no commas)
    val integerFormat = NumberFormat.getIntegerInstance().apply {
        isGroupingUsed = false
    }

    // Create a NumberFormatter using the integer format
    val numberFormatter = NumberFormatter(integerFormat).apply {
        valueClass = Int::class.java // Specify the value class as integer
        allowsInvalid = false // Prevent invalid characters
        minimum = Int.MIN_VALUE // Optional: Set minimum integer value
        maximum = Int.MAX_VALUE // Optional: Set maximum integer value
    }

    // Set the formatter factory for the JFormattedTextField
    formatterFactory = DefaultFormatterFactory(numberFormatter)

    // Set an initial value to match the integer type to avoid format issues
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