package com.shazdroid.cmsgen.cmsgenerator.custom_guis

import com.intellij.ui.JBColor
import com.shazdroid.cmsgen.cmsgenerator.viewmodel.KeyStatus
import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer


class KeyColumnRenderer(
    private val keyStatuses: Map<String, KeyStatus>
) : DefaultTableCellRenderer(), TableCellRenderer {

    // Mutable property to hold the current search text
    var searchText: String = ""
        set(value) {
            field = value
            // Optionally, trigger a repaint when searchText changes
            // This ensures that highlights are updated immediately
            // Note: Ensure this is called on the Event Dispatch Thread (EDT)
            SwingUtilities.invokeLater { this.parent?.repaint() }
        }

    private val keyLabel = JLabel()
    private val badgeLabel = JLabel()

    private val badgeDiameter = 16
    private val badgePadding = 5

    init {
        layout = BorderLayout()
        add(keyLabel, BorderLayout.CENTER)
        add(badgeLabel, BorderLayout.EAST)

        border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
        badgeLabel.horizontalAlignment = SwingConstants.RIGHT
    }

    fun calculateBadgeBounds(cellRect: Rectangle): Rectangle {
        val badgeX = cellRect.width - badgePadding - badgeDiameter
        val badgeY = (cellRect.height - badgeDiameter) / 2
        return Rectangle(badgeX, badgeY, badgeDiameter, badgeDiameter)
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        // Call the superclass method to get the default rendering
        val label = super.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column
        ) as JLabel

        val key = value as? String ?: ""

        // Set the text to just the key; badge will be drawn separately
        label.text = key

        return label
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val label = this as JLabel
        val key = extractPlainText(label.text)
        val status = keyStatuses[key]

        if (status != null) {
            val (badgeText, badgeColor) = when {
                status.isDuplicatedInEn || status.isDuplicatedInAr -> "D" to Color(242, 166, 34)
                status.isMissingInCmsKeyMapper -> "M" to Color(34, 166, 242)
                status.isMissingInEn || status.isMissingInAr -> "!" to Color(226, 61, 48)
                else -> "" to null
            }

            if (badgeText.isNotEmpty() && badgeColor != null) {
                val g2 = g as Graphics2D

                // Enable anti-aliasing for smoother graphics
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                val cellRect = label.getBounds()

                // Calculate badge bounds
                val badgeX = cellRect.width - badgePadding - badgeDiameter
                val badgeY = (cellRect.height - badgeDiameter) / 2

                // Draw badge background
                g2.color = badgeColor
                g2.fillOval(badgeX, badgeY, badgeDiameter, badgeDiameter)

                // Draw badge text
                g2.color = Color.WHITE // Use standard Color.WHITE
                g2.font = g2.font.deriveFont(Font.BOLD, 12f)

                val fm = g2.fontMetrics
                val textWidth = fm.stringWidth(badgeText)
                val textHeight = fm.getAscent()

                val textX = badgeX + (badgeDiameter - textWidth) / 2
                val textY = badgeY + ((badgeDiameter - fm.height) / 2) + fm.ascent

                g2.drawString(badgeText, textX, textY)

                // Optional: Logging for debugging
                println("Drawing badge for key: $key at ($badgeX, $badgeY)")
            }
        }
    }

    /**
     * Utility function to extract plain text from potentially HTML-formatted text.
     */
    private fun extractPlainText(htmlText: String): String {
        return if (htmlText.startsWith("<html>") && htmlText.endsWith("</html>")) {
            // Remove HTML tags
            htmlText.replace("<html>", "").replace("</html>", "")
                .replace("<span style='background: yellow;'>", "").replace("</span>", "")
        } else {
            htmlText
        }
    }

    private fun createBadgeIcon(text: String, color: Color, font: Font): Icon {
        return object : Icon {
            private val diameter: Int

            init {
                val adjustedFontSize = 9f
                val padding = 4
                val fm = Toolkit.getDefaultToolkit().getFontMetrics(font.deriveFont(Font.BOLD, adjustedFontSize))
                val textWidth = fm.stringWidth(text)
                val textHeight = fm.height
                diameter = (maxOf(textWidth, textHeight) + padding).coerceIn(14, 18)
            }

            override fun getIconWidth(): Int = diameter
            override fun getIconHeight(): Int = diameter

            override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
                if (g == null) return
                val g2 = g.create() as Graphics2D

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                val badgeX = x
                val badgeY = y + ((c?.height ?: 0) - diameter) / 2

                g2.color = color
                g2.fillOval(badgeX, badgeY, diameter, diameter)

                g2.color = Color.WHITE
                g2.font = font.deriveFont(Font.BOLD, 9f)
                val fm = g2.fontMetrics
                val textWidth = fm.stringWidth(text)
                val textX = badgeX + (diameter - textWidth) / 2
                val textY = badgeY + (diameter + fm.ascent - fm.descent) / 2 - 1

                g2.drawString(text, textX, textY)
                g2.dispose()
            }
        }
    }
}







