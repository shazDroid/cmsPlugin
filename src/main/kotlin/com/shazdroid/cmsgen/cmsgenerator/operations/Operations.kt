package com.shazdroid.cmsgen.cmsgenerator.operations

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.shazdroid.cmsgen.cmsgenerator.modifier.FileModifier
import com.shazdroid.cmsgen.cmsgenerator.modifier.JsonFileModifier
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService
import com.shazdroid.cmsgen.cmsgenerator.viewmodel.MainViewModel
import kotlinx.coroutines.*
import java.io.File
import java.util.regex.Pattern
import javax.swing.JOptionPane
import javax.swing.JTable
import kotlin.coroutines.CoroutineContext

class Operations(
    private val project: Project,
    private val fileService: FileSelectionService,
    private val fileModifier: FileModifier,
    private val jsonFileModifier: JsonFileModifier,
    private val viewModel: MainViewModel? = null
) {

    inner class AddOperations {
        fun addCmString(
            cmsKey: String,
            engContent: String,
            arContent: String,
            insertAtLine: Int = 0,
            selectedFile: String = ""
        ): Boolean {
            var isSuccess = false

            if (cmsKey.isNotEmpty() && engContent.isNotEmpty() && arContent.isNotEmpty()) {
                fileService.getSelectedFiles().first()
                fileService.getSelectedFiles().forEachIndexed { index, item ->
                    if (item.contains("CmsKeyMapper.kt")) {
                        when (fileModifier.appendCmsKeyToFile(item, cmsKey.trim(), project)) {
                            FileModifier.FileOperationResult.SUCCESS -> {
                                isSuccess = true
                            }

                            FileModifier.FileOperationResult.FILE_NOT_FOUND -> {
                                isSuccess = false
                            }

                            FileModifier.FileOperationResult.DUPLICATE_KEY -> {
                                isSuccess = false
                            }

                            FileModifier.FileOperationResult.COMPANION_OBJECT_NOT_FOUND -> {
                                isSuccess = false
                            }

                            FileModifier.FileOperationResult.WRITE_ERROR -> {
                                isSuccess = false
                            }
                        }
                    }
                }

                if (isSuccess) {
                    fileService.getSelectedFiles().forEachIndexed { _, item ->
                        if (item.contains("DefaultEn.json")) {
                            jsonFileModifier.appendToEnglishJson(
                                item,
                                cmsKey,
                                engContent.trim(),
                                project,
                                insertAtLine
                            )
                        }

                        if (item.contains("DefaultArabic.json")) {
                            jsonFileModifier.appendToArabicJson(
                                item, cmsKey, arContent.trim(), project,
                                insertAtLine
                            )
                        }
                    }
                }

                if (isSuccess) {
                    Messages.showMessageDialog("Cms key added successfully", "Success", Messages.getInformationIcon())
                    return true
                }

            } else {
                Messages.showMessageDialog("Please fill in all three inputs.", "Error", Messages.getErrorIcon())
                return false
            }

            return false
        }
    }

    inner class BulkParseOperations(){

    }


    inner class CompareOperations(private val table: JTable) : CoroutineScope {
        private val job = Job()
        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Main + job

        fun removeDuplicatesInFile(
            file: File?,
            key: String,
            project: Project,
            viewModel: MainViewModel
        ) {
            if (file == null || !file.exists()) {
                JOptionPane.showMessageDialog(null, "File not found.")
                return
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val lines = withContext(Dispatchers.IO) { file.readLines() }
                    val regex = Pattern.compile("\"(.*?)\"\\s*:\\s*\"(.*?)\"")

                    val occurrenceIndices = mutableListOf<Int>()
                    for ((index, line) in lines.withIndex()) {
                        val matcher = regex.matcher(line)
                        if (matcher.find()) {
                            val lineKey = matcher.group(1)
                            if (lineKey == key) {
                                occurrenceIndices.add(index)
                            }
                        }
                    }


                    if (occurrenceIndices.size <= 1) {
                        JOptionPane.showMessageDialog(null, "No duplicates found for key: $key.")
                        return@launch
                    }

                    val linesToRemove = occurrenceIndices.dropLast(1)


                    val cleanedLines = lines.toMutableList()

                    for (index in linesToRemove.sortedDescending()) {
                        cleanedLines.removeAt(index)
                    }

                    withContext(Dispatchers.IO) {
                        file.writeText(cleanedLines.joinToString("\n"))
                    }

                    viewModel.refreshFile(project, file.path)
                } catch (e: Exception) {
                    e.printStackTrace()
                    JOptionPane.showMessageDialog(null, "Error removing duplicates for key: $key.")
                }
            }
        }

        fun cancelOperations() {
            job.cancel() // Cancels all ongoing operations
        }
    }

}