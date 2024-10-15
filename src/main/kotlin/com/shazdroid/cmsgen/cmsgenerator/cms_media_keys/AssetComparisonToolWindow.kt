package com.shazdroid.cmsgen.cmsgenerator.cms_media_keys


import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.shazdroid.cmsgen.cmsgenerator.util.icon
import org.jetbrains.kotlin.psi.*
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class AssetsComparisonToolWindow(private val project: Project) : SimpleToolWindowPanel(true, true) {

    private val tableModel = DefaultTableModel()
    private val table = JBTable(tableModel)
    private val tableData = mutableListOf<TableRowData>()

    init {
        tableModel.addColumn("Type")
        tableModel.addColumn("Name")
        tableModel.addColumn("Action")
        table.setDefaultEditor(Any::class.java, null)

        table.columnModel.getColumn(0).preferredWidth = 100 // "Type" column
        table.columnModel.getColumn(1).preferredWidth = 200 // "Name" column
        table.columnModel.getColumn(2).preferredWidth = 50  // "Action" column

        table.setShowGrid(true)
        table.autoscrolls = true
        table.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN

        val scrollPane = JBScrollPane(table)

        val refreshAction = object : AnAction("Refresh", "Refresh the assets comparison", null) {
            override fun actionPerformed(e: AnActionEvent) {
                refreshData()
            }
        }

        val actionGroup = DefaultActionGroup(refreshAction)
        val actionToolbar: ActionToolbar =
            ActionManager.getInstance().createActionToolbar("AssetsComparisonToolbar", actionGroup, false)
        actionToolbar.setTargetComponent(this)

        val panel = JPanel(BorderLayout())
        panel.add(actionToolbar.component, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)

        setContent(panel)

        refreshData()

        val missingIcon = IconLoader.getIcon("missing_asset.svg".icon(), javaClass)
        val unusedIcon = IconLoader.getIcon("unused_asset.svg".icon(), javaClass)

        table.columnModel.getColumn(2).cellRenderer = ButtonRenderer()
        table.columnModel.getColumn(2).cellEditor = ButtonEditor(project, tableData)

        table.columnModel.getColumn(0).cellRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable,
                value: Any?,
                isSelected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int
            ): Component {
                val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

                if (value == "Missing Asset") {
                    text = value.toString()
                    icon = missingIcon
                } else {
                    text = value?.toString() ?: ""
                    icon = unusedIcon
                }

                return component
            }
        }


        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val point = e.point
                val row = table.rowAtPoint(point)
                val column = table.columnAtPoint(point)
                if (row >= 0 && (column == 0 || column == 1)) {
                    val rowData = tableData[row]
                    if (rowData.type == "Missing Asset") {
                        addConstToCmsMediaKeys(rowData.name)
                    }
                }
            }
        })
    }


    private fun findOrCreateCompanionObject(cmsMediaKeysFile: KtFile): KtObjectDeclaration? {
        val declarations = cmsMediaKeysFile.declarations
        val cmsMediaKeysClass = declarations.filterIsInstance<KtClass>().firstOrNull { it.name == "CmsMediaKeys" }

        if (cmsMediaKeysClass != null) {
            var companionObject = cmsMediaKeysClass.companionObjects.firstOrNull()
            if (companionObject == null) {
                val factory = KtPsiFactory(cmsMediaKeysFile.project)
                companionObject = factory.createCompanionObject()
                cmsMediaKeysClass.addDeclaration(companionObject)
            }
            return companionObject
        }
        return null
    }

    private fun addConstToCmsMediaKeys(constantName: String) {
        ApplicationManager.getApplication().invokeLater {
            val confirmed = Messages.showYesNoDialog(
                project,
                "Do you want to add the constant \"$constantName\" to CmsMediaKeys.kt?",
                "Add Constant",
                Messages.getQuestionIcon()
            ) == Messages.YES

            if (!confirmed) return@invokeLater

            val psiManager = PsiManager.getInstance(project)
            var cmsMediaKeysFile = AssetsComparator.findCmsMediaKeysFile(project)

            if (cmsMediaKeysFile == null) {
                val createFileConfirmed = Messages.showYesNoDialog(
                    project,
                    "CmsMediaKeys.kt not found. Do you want to create it?",
                    "Create File",
                    Messages.getQuestionIcon()
                ) == Messages.YES

                if (!createFileConfirmed) return@invokeLater

                val sourceRoot = ProjectRootManager.getInstance(project).contentSourceRoots.firstOrNull()
                if (sourceRoot == null) {
                    Messages.showErrorDialog(project, "No source root found to create CmsMediaKeys.kt.", "Error")
                    return@invokeLater
                }

                val packageName = ""
                val file = sourceRoot.createChildData(this, "CmsMediaKeys.kt")
                val fileContent = buildString {
                    if (packageName.isNotEmpty()) {
                        append("package $packageName\n\n")
                    }
                    append(
                        """
                    annotation class CmsMediaKeys {
                        companion object {
                        }
                    }
                    """.trimIndent()
                    )
                }
                VfsUtil.saveText(file, fileContent)
                cmsMediaKeysFile = psiManager.findFile(file) as? KtFile
                if (cmsMediaKeysFile == null) {
                    Messages.showErrorDialog(project, "Failed to create CmsMediaKeys.kt.", "Error")
                    return@invokeLater
                }
            }

            WriteCommandAction.runWriteCommandAction(project) {
                val companionObject = findOrCreateCompanionObject(cmsMediaKeysFile)
                if (companionObject != null) {
                    val factory = KtPsiFactory(project)
                    val constantNameUpper = constantName.uppercase().replace(" ", "_")
                    val property = factory.createDeclaration<KtProperty>("const val $constantNameUpper = \"$constantName\"")
                    companionObject.addDeclaration(property)
                    // Reformat the code
                    CodeStyleManager.getInstance(project).reformat(companionObject)
                    // Bring the file to focus
                    OpenFileDescriptor(project, cmsMediaKeysFile.virtualFile).navigate(true)
                } else {
                    Messages.showErrorDialog(project, "Failed to find or create companion object in CmsMediaKeys.kt.", "Error")
                }
            }

            refreshData()
        }
    }


    private fun refreshData() {
        tableModel.setRowCount(0)
        tableData.clear()

        val comparisonResult = AssetsComparator.compareAssetsWithKeys(project)

        for (asset in comparisonResult.missingAssets) {
            val rowData = TableRowData("Missing Asset", asset.name, asset.path)
            tableData.add(rowData)
            tableModel.addRow(arrayOf(rowData.type, rowData.name, "View"))
        }

        for (key in comparisonResult.unusedKeys) {
            val rowData = TableRowData("Unused Key", key.name, key.location)
            tableData.add(rowData)
            tableModel.addRow(arrayOf(rowData.type, rowData.name, "View"))
        }
    }
}

data class TableRowData(
    val type: String,
    val name: String,
    val path: String
)
