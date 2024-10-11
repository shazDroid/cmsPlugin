package com.shazdroid.cmsgen.cmsgenerator.custom_guis

import com.intellij.ui.JBColor
import com.shazdroid.cmsgen.cmsgenerator.viewmodel.KeyStatus
import java.awt.*
import javax.swing.*
import javax.swing.table.TableCellRenderer


class KeyColumnRenderer(
    private val keyStatuses: Map<String, KeyStatus>
) : JPanel(), TableCellRenderer {

    private val keyLabel = JLabel()
    private val badgeLabel = JLabel()

    init {
        layout = BorderLayout()
        add(keyLabel, BorderLayout.CENTER)
        add(badgeLabel, BorderLayout.EAST)

        border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
        badgeLabel.horizontalAlignment = SwingConstants.RIGHT
    }

    override fun getTableCellRendererComponent(
        table: JTable, value: Any?, isSelected: Boolean,
        hasFocus: Boolean, row: Int, column: Int
    ): Component {
        val key = value as? String ?: ""
        val status = keyStatuses[key]

        keyLabel.text = key
        keyLabel.font = table.font

        if (status != null) {
            val (badgeText, badgeColor) = when {
                status.isDuplicatedInEn || status.isDuplicatedInAr -> "D" to Color.YELLOW
                status.isMissingInCmsKeyMapper -> "M" to Color.ORANGE
                status.isMissingInEn || status.isMissingInAr -> "!" to Color.RED
                else -> "" to null
            }

            if (badgeColor != null && badgeText.isNotEmpty()) {
                badgeLabel.icon = createBadgeIcon(badgeText, badgeColor, table.font)
                badgeLabel.text = ""
                badgeLabel.toolTipText = when (badgeText) {
                    "D" -> "Duplicate key detected"
                    "M" -> "Key missing in CmsKeyMapper.kt"
                    "!" -> "Key missing in one of the JSON files"
                    else -> null
                }
            } else {
                badgeLabel.icon = null
                badgeLabel.text = ""
                badgeLabel.toolTipText = null
            }
        } else {
            badgeLabel.icon = null
            badgeLabel.text = ""
            badgeLabel.toolTipText = null
        }

        // Handle selection background and foreground
        if (isSelected) {
            background = table.selectionBackground
            keyLabel.foreground = table.selectionForeground
            badgeLabel.foreground = table.selectionForeground
        } else {
            background = table.background
            keyLabel.foreground = table.foreground
            badgeLabel.foreground = table.foreground
        }

        return this
    }

    fun getBadgeBounds(): Rectangle? {
        badgeLabel.doLayout()
        badgeLabel.validate()
        val badgeRect = badgeLabel.bounds
        return badgeRect
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







