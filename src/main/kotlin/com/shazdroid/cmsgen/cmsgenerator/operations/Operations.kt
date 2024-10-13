package com.shazdroid.cmsgen.cmsgenerator.operations

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.opencsv.CSVReader
import com.shazdroid.cmsgen.cmsgenerator.keycomparison.KeyComparisonTable
import com.shazdroid.cmsgen.cmsgenerator.modifier.FileModifier
import com.shazdroid.cmsgen.cmsgenerator.modifier.JsonFileModifier
import com.shazdroid.cmsgen.cmsgenerator.storage.FileSelectionService
import com.shazdroid.cmsgen.cmsgenerator.viewmodel.MainViewModel
import kotlinx.coroutines.*
import java.io.File
import java.io.FileReader
import java.util.regex.Pattern
import javax.swing.JOptionPane
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import kotlin.coroutines.CoroutineContext

class Operations(
    private val project: Project,
    private val fileService: FileSelectionService,
    private val fileModifier: FileModifier,
    private val jsonFileModifier: JsonFileModifier,
    private val viewModel: MainViewModel? = null
) {
    val enEntries = viewModel?.readJsonAsList(viewModel.getEnglishJsonFile())
    val arEntries = viewModel?.readJsonAsList(viewModel.getArabicJsonFile())
    val cmsKeys = viewModel?.getKeysFromCmsKeyMapper()

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

        fun replaceKeyInEnglishFile(key: String, enValue: String) {
            val enFile = viewModel?.getEnglishJsonFile()
            if (enFile != null && enFile.exists()) {
                val objectMapper = ObjectMapper().registerModule(KotlinModule())
                val typeRef = object : TypeReference<MutableMap<String, String>>() {}

                val enMap: MutableMap<String, String> = objectMapper.readValue(enFile, typeRef)

                if (enMap.containsKey(key)) {
                    enMap[key] = enValue

                    enFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(enMap))
                    viewModel?.refreshFile(project, enFile.path)
                }
            }
        }

        fun replaceKeyInArabicFile(key: String, arValue: String) {
            val arFile = viewModel?.getArabicJsonFile()
            if (arFile != null && arFile.exists()) {
                val objectMapper = ObjectMapper().registerModule(KotlinModule())
                val typeRef = object : TypeReference<MutableMap<String, String>>() {}

                val arMap: MutableMap<String, String> = objectMapper.readValue(arFile, typeRef)

                if (arMap.containsKey(key)) {
                    arMap[key] = arValue

                    arFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arMap))
                    viewModel?.refreshFile(project, arFile.path)
                }
            }
        }

        fun camelToSnakeCase(input: String): String {
            val cleanedInput = input.trim().replace(Regex("\\s+"), " ")

            return if (cleanedInput.contains(" ")) {
                cleanedInput
                    .replace(" ", "")
                    .replace(Regex("([a-z])([A-Z])"), "$1_$2")
                    .uppercase()
            } else {

                cleanedInput
                    .replace(Regex("([a-z])([A-Z])"), "$1_$2")
                    .uppercase()
            }
        }

        fun replaceKeyInCmsKeyMapper(key: String) {
            val cmsKeyFile = viewModel?.getCmsKeyMapperFile()
            if (cmsKeyFile != null && cmsKeyFile.exists()) {
                val fileContent = cmsKeyFile.readText()

                val keyUpperCase = camelToSnakeCase(key)

                val companionObjectPattern = Regex("(companion\\s+object\\s*\\{)([\\s\\S]*?)(\\})")
                val matchResult = companionObjectPattern.find(fileContent)

                if (matchResult != null) {
                    val beforeCompanion = fileContent.substring(0, matchResult.range.first)
                    val insideCompanion = matchResult.groups[2]?.value?.trimEnd() ?: ""
                    val afterCompanion = fileContent.substring(matchResult.range.last + 1)

                    val updatedCompanion = insideCompanion.replace(
                        Regex("const val $keyUpperCase = \".*?\""),
                        "const val $keyUpperCase = \"$key\""
                    )

                    val updatedContent =
                        beforeCompanion + matchResult.groups[1]?.value + updatedCompanion + "\n    }" + afterCompanion

                    cmsKeyFile.writeText(updatedContent)
                    viewModel?.refreshFile(project, cmsKeyFile.path)
                }
            }
        }

    }


    inner class CompareOperations(private val table: JTable) : CoroutineScope {
        private val job = Job()
        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Main + job

        fun removeDuplicatesInFile(
            file: File?,
            key: String,
            project: Project,
            viewModel: MainViewModel,
            keyComparisonTable: KeyComparisonTable
        ) {
            if (file == null || !file.exists()) {
                Messages.showDialog(
                    null,
                    "File not found.",
                    "Error",
                    arrayOf("OK"),
                    0,
                    Messages.getErrorIcon()
                )
                return
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val lines = withContext(Dispatchers.IO) { file.readLines() }
                    val regex = Pattern.compile("\"(.*?)\"\\s*:\\s*\"(.*?)\"")

                    val occurrenceIndices = mutableListOf<Int>()
                    var lastValue: String? = null
                    for ((index, line) in lines.withIndex()) {
                        val matcher = regex.matcher(line)
                        if (matcher.find()) {
                            val lineKey = matcher.group(1)
                            val lineValue = matcher.group(2)
                            if (lineKey == key) {
                                occurrenceIndices.add(index)
                                lastValue = lineValue
                            }
                        }
                    }

                    if (occurrenceIndices.size <= 1) {
                        Messages.showDialog(
                            null,
                            "No duplicates found for key: $key.",
                            "Duplicate Removal",
                            arrayOf("OK"),
                            0,
                            Messages.getInformationIcon()
                        )
                        return@launch
                    }

                    val linesToRemove = occurrenceIndices.dropLast(1)  // Keep the last occurrence

                    val cleanedLines = lines.toMutableList()

                    for (index in linesToRemove.sortedDescending()) {
                        cleanedLines.removeAt(index)
                    }

                    withContext(Dispatchers.IO) {
                        file.writeText(cleanedLines.joinToString("\n"))
                    }

                    viewModel.refreshFile(project, file.path)

                    keyComparisonTable.updateTableRowAfterOperation(key, lastValue, lastValue)

                    keyComparisonTable.reSortTable()

                    Messages.showDialog(
                        null,
                        "Duplicates for key '$key' have been removed.",
                        "Duplicate Removal",
                        arrayOf("OK"),
                        0,
                        Messages.getInformationIcon()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    Messages.showDialog(
                        null,
                        "Error removing duplicates for key: $key.",
                        "Error",
                        arrayOf("OK"),
                        0,
                        Messages.getErrorIcon()
                    )
                }
            }
        }


        fun cancelOperations() {
            job.cancel()
        }
    }

    inner class BulkAddOperations {
        val addOperations = AddOperations()

        fun parseCsvFileToTable(csvFile: File, table: JTable): List<Array<String>> {
            val csvReader = CSVReader(FileReader(csvFile))
            val allRows = mutableListOf<Array<String>>()

            csvReader.forEach { row ->
                allRows.add(row)
            }


            val headers = allRows.firstOrNull() ?: emptyArray()
            val tableModel = DefaultTableModel(headers, 0)
            allRows.drop(1).forEach { row -> tableModel.addRow(row) }
            table.model = tableModel

            table.revalidate()
            table.repaint()

            return allRows
        }



        fun processBulkKeysFromCsv(
            csvData: List<Array<String>>,
            cmsKeyColumnIndex: Int,
            englishColumnIndex: Int,
            arabicColumnIndex: Int,
            viewModel: MainViewModel
        ) {
            var userChoice: BulkActionChoice? = null

            csvData.forEach { row ->
                val cmsKey = row[cmsKeyColumnIndex].trim()
                val englishValue = row[englishColumnIndex].trim()
                val arabicValue = row[arabicColumnIndex].trim()

                if (cmsKey.isNotEmpty()) {
                    if (viewModel.isDuplicateKey(cmsKey)) {
                        when (userChoice) {
                            BulkActionChoice.SKIP_ALL -> {
                                println("Skipped all duplicates for key: $cmsKey")
                                return@forEach
                            }
                            BulkActionChoice.REPLACE_ALL -> {
                                replaceKeyInFiles(cmsKey, englishValue, arabicValue)
                                println("Replaced all duplicates for key: $cmsKey")
                                return@forEach
                            }
                            else -> {
                                val options = arrayOf("Skip", "Replace", "Skip All", "Replace All")
                                val result = Messages.showDialog(
                                    "Duplicate Key Detected for '$cmsKey'. What would you like to do?",
                                    "Duplicate Key",
                                    options,
                                    0,
                                    Messages.getWarningIcon()
                                )

                                when (result) {
                                    0 -> {
                                        // Skip this duplicate key
                                        println("Skipped duplicate for key: $cmsKey")
                                    }
                                    1 -> {
                                        // Replace this duplicate key
                                        replaceKeyInFiles(cmsKey, englishValue, arabicValue)
                                        println("Replaced duplicate for key: $cmsKey")
                                    }
                                    2 -> {
                                        // Skip All future duplicate keys
                                        userChoice = BulkActionChoice.SKIP_ALL
                                        println("Skipped all duplicates for key: $cmsKey")
                                    }
                                    3 -> {
                                        // Replace All future duplicate keys
                                        userChoice = BulkActionChoice.REPLACE_ALL
                                        replaceKeyInFiles(cmsKey, englishValue, arabicValue)
                                        println("Replaced all duplicates for key: $cmsKey")
                                    }
                                }
                            }
                        }
                    } else {
                        // If the key doesn't exist, add it
                        addKeyToFiles(cmsKey, englishValue, arabicValue)
                        println("Added key: $cmsKey with English: $englishValue, Arabic: $arabicValue")
                    }
                }
            }
        }


        fun addKeyToFiles(key: String, enValue: String, arValue: String) {
            addOperations.addCmString(key, enValue, arValue)
        }

        private fun replaceKeyInFiles(key: String, enValue: String, arValue: String) {
            addOperations.replaceKeyInEnglishFile(key, enValue)
            addOperations.replaceKeyInArabicFile(key, arValue)
            addOperations.replaceKeyInCmsKeyMapper(key)
        }

        private fun parseCsvFile(file: File): List<Triple<String, String, String>> {
            val csvEntries = mutableListOf<Triple<String, String, String>>()
            file.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size == 3) {
                    csvEntries.add(Triple(parts[0].trim(), parts[1].trim(), parts[2].trim()))
                }
            }
            return csvEntries
        }

        fun readCsvAsTableModel(csvFilePath: String): DefaultTableModel {
            val reader = CSVReader(FileReader(csvFilePath))
            val csvContent = reader.readAll()

            if (csvContent.isEmpty()) {
                return DefaultTableModel()
            }

            val columnNames = csvContent[0]

            val dataRows = csvContent.subList(1, csvContent.size).map { it }.toTypedArray()

            return DefaultTableModel(dataRows, columnNames)
        }

    }

    companion object {
        enum class BulkActionChoice {
            SKIP,
            REPLACE,
            SKIP_ALL,
            REPLACE_ALL
        }
    }

}