package com.shazdroid.cmsgen.cmsgenerator.viewmodel

data class PreparedTableData(
    val data: Array<Array<Any?>>,
    val columnNames: Array<String>,
    val keyStatuses: Map<String, KeyStatus>,
    val cmsKeys: Set<String>
)
