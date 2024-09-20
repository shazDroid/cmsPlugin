package com.shazdroid.cmsgen.cmsgenerator.models

data class FileSelectionState(
    var selectedFiles: List<String> = mutableListOf()  // List of file paths as strings
)