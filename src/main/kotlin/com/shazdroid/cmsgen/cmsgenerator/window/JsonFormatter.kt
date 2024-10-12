package com.shazdroid.cmsgen.cmsgenerator.window

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.gson.GsonBuilder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.uiDesigner.core.GridConstraints
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.text.SimpleAttributeSet

class JsonFormatter(private val project: Project) : JPanel() {
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
    private lateinit var formatterPanel: JPanel
    private lateinit var formatButton: JButton
    private val statusLabel = JLabel("Ready")
    lateinit var mainPanel: JPanel
    private var isFormatting = false

    val jsonEditorPanel = JsonEditorPanelWithFormatButton(project)
    private val frame = JFrame()


    init {
        setupUI()
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                jsonEditorPanel.disposeEditor()
                frame.dispose()
            }
        })
        formatButton.addActionListener {
            jsonEditorPanel.formatJsonContent(project)
        }
    }


    private fun setupUI() {
        jsonEditorPanel.minimumSize = Dimension(500, 200)
        jsonEditorPanel.preferredSize = Dimension(600, 400)
        jsonEditorPanel.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)

        val constraints = GridConstraints()

        constraints.row = 0
        constraints.column = 0
        constraints.rowSpan = 1
        constraints.colSpan = 1
        constraints.fill = GridConstraints.FILL_BOTH
        constraints.hSizePolicy = GridConstraints.SIZEPOLICY_CAN_GROW
        constraints.vSizePolicy = GridConstraints.SIZEPOLICY_CAN_GROW

        formatterPanel.add(jsonEditorPanel, constraints)

        frame.title = "CMS Input"
        frame.contentPane = mainPanel
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.pack()
    }


    class JsonEditorPanelWithFormatButton(project: Project) : JPanel() {
        private var editor: Editor

        init {
            layout = BorderLayout()

            val document: Document = EditorFactory.getInstance().createDocument("")

            val jsonFileType = FileTypeManager.getInstance().getFileTypeByExtension("json")
            editor = EditorFactory.getInstance().createEditor(document, project, jsonFileType, false)

            add(editor.component, BorderLayout.CENTER)

        }

        fun formatJsonContent(project: Project) {
            try {
                val rawJson = editor.document.text
                if (rawJson.isBlank()) {
                    JOptionPane.showMessageDialog(this, "No JSON content to format", "Error", JOptionPane.ERROR_MESSAGE)
                    return
                }

                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonElement = gson.fromJson(rawJson, Any::class.java)
                val prettyJson = gson.toJson(jsonElement)


                WriteCommandAction.runWriteCommandAction(project) {
                    editor.document.setText(prettyJson)
                }

            } catch (e: com.google.gson.JsonSyntaxException) {
                JOptionPane.showMessageDialog(
                    this,
                    "Invalid JSON format: ${e.message}",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
                e.printStackTrace()
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    this,
                    "An unexpected error occurred: ${e.message}",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
                e.printStackTrace()
            }
        }

        // Clean up the editor resources
        fun disposeEditor() {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }
}
