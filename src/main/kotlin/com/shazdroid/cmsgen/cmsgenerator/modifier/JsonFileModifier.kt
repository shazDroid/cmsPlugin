package com.shazdroid.cmsgen.cmsgenerator.modifier


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File

class JsonFileModifier {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule())

    fun appendToEnglishJson(enFilePath: String, cmsKey: String, enContent: String) {
        val enFile = File(enFilePath)

        // Read the existing content
        val enJson: MutableMap<String, String> = if (enFile.exists() && enFile.length() > 0) {
            objectMapper.readValue(enFile)
        } else {
            mutableMapOf()
        }

        // Append the new key-value pair
        enJson[cmsKey] = enContent

        // Write the updated content back while preserving formatting
        enFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(enJson))

        println("Content successfully appended to DefaultEn.json.")
    }

    fun appendToArabicJson(arFilePath: String, cmsKey: String, arContent: String) {
        val arFile = File(arFilePath)

        // Read the existing content
        val arJson: MutableMap<String, String> = if (arFile.exists() && arFile.length() > 0) {
            objectMapper.readValue(arFile)
        } else {
            mutableMapOf()
        }

        // Append the new key-value pair
        arJson[cmsKey] = arContent

        // Write the updated content back while preserving formatting
        arFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arJson))

        println("Content successfully appended to DefaultAr.json.")
    }
}
