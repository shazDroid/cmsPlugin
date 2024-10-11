package com.shazdroid.cmsgen.cmsgenerator.custom_guis
import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer

class BadgeCellRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable, value: Any?, isSelected: Boolean,
        hasFocus: Boolean, row: Int, column: Int
    ): Component {
        // Call the superclass method to get the default rendering
        val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel

        // Reset any customizations
        c.icon = null
        c.text = ""

        // Get the count value
        val count = value as? Int ?: 0

        // Determine the badge color based on the count
        val badgeColor = when {
            count == 0 -> Color.RED      // Missing key
            count > 1 -> Color.YELLOW    // Duplicate key
            else -> null                 // Normal
        }

        // Create the badge icon if necessary
        if (badgeColor != null) {
            c.icon = createBadgeIcon(count, badgeColor, c.font)
        } else {
            c.text = count.toString()
        }

        // Handle selection background and foreground
        if (isSelected) {
            c.background = table.selectionBackground
            c.foreground = table.selectionForeground
        } else {
            c.background = table.background
            c.foreground = table.foreground
        }

        c.horizontalAlignment = SwingConstants.CENTER

        return c
    }

    private fun createBadgeIcon(count: Int, color: Color, font: Font): Icon {
        return object : Icon {
            override fun getIconWidth(): Int = 30
            override fun getIconHeight(): Int = 20

            override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
                if (g == null) return
                val g2 = g.create() as Graphics2D

                // Enable anti-aliasing
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                // Draw the badge background
                g2.color = color
                g2.fillRoundRect(x + 5, y + 3, 20, 14, 10, 10)

                // Draw the count text
                g2.color = Color.BLACK
                g2.font = font.deriveFont(Font.BOLD, 12f)
                val fm = g2.fontMetrics
                val text = count.toString()
                val textWidth = fm.stringWidth(text)
                val textX = x + 15 - textWidth / 2
                val textY = y + 3 + ((14 + fm.ascent) / 2) - 3

                g2.drawString(text, textX, textY)

                g2.dispose()
            }
        }
    }
}
