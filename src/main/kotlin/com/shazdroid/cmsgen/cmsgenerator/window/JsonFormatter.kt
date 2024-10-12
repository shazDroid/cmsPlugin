package com.shazdroid.cmsgen.cmsgenerator.window

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.*

class JsonFormatter : JPanel() {
    private val objectMapper = ObjectMapper().registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.SingletonSupport, false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    )
    private val styles = mutableMapOf<String, SimpleAttributeSet>()
    private lateinit var searchFieldText: JTextField
    private lateinit var searchLabel: JLabel
    private val debounceDelay = 500 // milliseconds
    private var debounceTimer: Timer? = null

    private lateinit var inputTextPane: JTextPane


    private lateinit var formatButton: JButton
    private val statusLabel = JLabel("Ready")
    lateinit var mainPanel: JPanel
    private var isFormatting = false


    private val frame = JFrame()


    init {
        setupUI()
        setupStyles()
        setupListeners()
    }

    private fun setupStyles() {
        inputTextPane.border = BorderFactory.createLineBorder(JBColor.GRAY, 1)

        // Key Style
        val keyStyle = SimpleAttributeSet()
        StyleConstants.setForeground(keyStyle, UIManager.getColor("Editor.colors.key") ?: JBColor.BLUE)
        StyleConstants.setBold(keyStyle, true)
        styles["key"] = keyStyle

        // String Style
        val stringStyle = SimpleAttributeSet()
        StyleConstants.setForeground(stringStyle, UIManager.getColor("Editor.colors.string") ?: JBColor.ORANGE)
        styles["string"] = stringStyle

        // Number Style
        val numberStyle = SimpleAttributeSet()
        StyleConstants.setForeground(numberStyle, UIManager.getColor("Editor.colors.number") ?: JBColor.MAGENTA)
        styles["number"] = numberStyle

        // Boolean Style
        val booleanStyle = SimpleAttributeSet()
        StyleConstants.setForeground(booleanStyle, UIManager.getColor("Editor.colors.boolean") ?: Color.GREEN.darker())
        styles["boolean"] = booleanStyle

        // Null Style
        val nullStyle = SimpleAttributeSet()
        StyleConstants.setForeground(nullStyle, UIManager.getColor("Editor.colors.null") ?: JBColor.RED)
        styles["null"] = nullStyle

        // Bracket Style
        val bracketStyle = SimpleAttributeSet()
        StyleConstants.setForeground(bracketStyle, UIManager.getColor("Editor.colors.bracket") ?: JBColor.GRAY)
        styles["bracket"] = bracketStyle

        // Verify all styles are initialized
        verifyStyles()
    }

    private fun verifyStyles() {
        val requiredStyles = listOf("key", "string", "number", "boolean", "null", "bracket")
        requiredStyles.forEach { styleName ->
            if (!styles.containsKey(styleName)) {
                println("Style '$styleName' is not initialized!")
            }
        }
    }


    private fun setupUI() {
        frame.title = "CMS Input"
        frame.contentPane = mainPanel
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.pack()

        inputTextPane.background = Gray._43
    }

    private fun setupListeners() {
        // Document Listener for Paste Detection
        inputTextPane.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                if (isFormatting) return
                debounceFormat()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                if (isFormatting) return
                debounceFormat()
            }

            override fun changedUpdate(e: DocumentEvent?) {}
        })

        // Format Button Action
        formatButton.addActionListener {
            // formatJson()
        }

        searchFieldText.addActionListener {
            searchAndHighlight()
        }
    }

    private fun debounceFormat() {
        debounceTimer?.stop()
        debounceTimer = Timer(debounceDelay) {
            //   formatJson()
            formatAndHighlightJson(inputTextPane, inputTextPane.text)
        }.apply {
            isRepeats = false
            start()
        }
    }

    private fun handlePaste(e: DocumentEvent?) {
        if (e == null) return
        // formatJson()
    }

    fun formatAndHighlightJson(textPane: JTextPane, jsonText: String) {
        val objectMapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        try {
            // Format the JSON using Jackson
            val jsonNode = objectMapper.readTree(jsonText)
            val formattedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode)

            // Apply syntax highlighting
            applySyntaxHighlighting(textPane, formattedJson)
        } catch (e: Exception) {
            textPane.text = "Invalid JSON"
        }
    }

    private var lastHighlightedText = ""

    fun applySyntaxHighlighting(textPane: JTextPane, formattedJson: String, searchText: String = "") {
        val doc = textPane.styledDocument
        val defaultFont = Font("Courier", Font.PLAIN, 14)

        // Save caret and selection positions before modifying the document
        val caretPosition = textPane.caretPosition
        val selectionStart = textPane.selectionStart
        val selectionEnd = textPane.selectionEnd

        // Remove existing content
        doc.remove(0, doc.length)

        // Define attributes for JSON highlighting
        val keyStyle = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color(0, 0, 255)) // Blue for keys
            StyleConstants.setBold(this, true)
            StyleConstants.setFontFamily(this, defaultFont.family)
        }
        val stringStyle = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color(34, 139, 34)) // Green for strings
            StyleConstants.setFontFamily(this, defaultFont.family)
        }
        val numberStyle = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color(255, 69, 0)) // Orange for numbers
            StyleConstants.setFontFamily(this, defaultFont.family)
        }
        val booleanStyle = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color(148, 0, 211)) // Purple for booleans
            StyleConstants.setFontFamily(this, defaultFont.family)
        }
        val braceStyle = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color(0, 0, 0)) // Black for braces
            StyleConstants.setFontFamily(this, defaultFont.family)
        }
        val colonStyle = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color(128, 128, 128)) // Gray for colon
            StyleConstants.setFontFamily(this, defaultFont.family)
        }

        var offset = 0
        var insideString = false
        var isKey = true // Used to alternate between key and value in JSON
        var currentStyle: AttributeSet = braceStyle

        // Apply syntax highlighting to each character in the JSON
        formattedJson.forEach { char ->
            when {
                char == '"' && !insideString -> {
                    insideString = true
                    currentStyle = if (isKey) keyStyle else stringStyle
                }
                char == '"' && insideString -> {
                    insideString = false
                    isKey = !isKey // Toggle between key and value
                }
                insideString -> {
                    // Stay in the current string style (key or value)
                }
                char.isDigit() || char == '-' -> {
                    currentStyle = numberStyle
                }
                char == 't' || char == 'f' || char == 'n' -> {
                    // Detect booleans (true/false) and null
                    currentStyle = booleanStyle
                }
                char == '{' || char == '}' || char == '[' || char == ']' -> {
                    currentStyle = braceStyle
                }
                char == ':' -> {
                    currentStyle = colonStyle
                }
            }

            // Insert the character with its respective style
            doc.insertString(offset, char.toString(), currentStyle)
            offset++
        }

        // Handle search highlighting
        if (searchText.isNotEmpty()) {
            val text = textPane.text
            var searchIndex = text.indexOf(searchText, ignoreCase = true)
            while (searchIndex >= 0) {
                val highlightStyle = SimpleAttributeSet().apply {
                    StyleConstants.setBackground(this, Color.YELLOW) // Highlight color
                    StyleConstants.setFontFamily(this, defaultFont.family)
                }
                doc.setCharacterAttributes(searchIndex, searchText.length, highlightStyle, false)
                searchIndex = text.indexOf(searchText, searchIndex + searchText.length, ignoreCase = true)
            }
        }

        // Restore caret and selection positions
        textPane.caretPosition = caretPosition
        textPane.select(selectionStart, selectionEnd)
    }


    private fun searchAndHighlight() {
        val searchTerm = searchFieldText.text.trim()
        if (searchTerm.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search term.", "Info", JOptionPane.INFORMATION_MESSAGE)
            return
        }

        // Remove previous highlights
        inputTextPane.highlighter.removeAllHighlights()

        val content = inputTextPane.text
        var index = content.indexOf(searchTerm, ignoreCase = true)
        var count = 0
        val highlighter = inputTextPane.highlighter
        val painter = DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW)

        while (index >= 0) {
            try {
                highlighter.addHighlight(index, index + searchTerm.length, painter)
                count++
                index = content.indexOf(searchTerm, index + searchTerm.length, ignoreCase = true)
            } catch (e: BadLocationException) {
                e.printStackTrace()
            }
        }

        statusLabel.text = "Status: Found $count match(es)."
        if (count == 0) {
            JOptionPane.showMessageDialog(
                this,
                "No matches found for \"$searchTerm\".",
                "Info",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    class CodeViewerDialog(private val project: Project, private val codeContent: String) : DialogWrapper(true) {

        private var editor: Editor? = null

        init {
            title = "Code Viewer"
            init() // This will call createCenterPanel
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(VerticalFlowLayout())

            // Create a document to hold the code content
            val editorFactory = EditorFactory.getInstance()
            val document: Document = editorFactory.createDocument(codeContent)

            // Create the editor
            editor = editorFactory.createViewer(document, project)

            // Customize editor settings (optional)
            (editor as EditorEx).isViewer = true
            (editor as EditorEx).scrollingModel.disableAnimation() // Disable scrolling animation

            panel.add(editor!!.component)
            return panel
        }

        override fun dispose() {
            // Dispose the editor when the dialog is closed
            editor?.let {
                EditorFactory.getInstance().releaseEditor(it)
            }
            super.dispose()
        }
    }
}
