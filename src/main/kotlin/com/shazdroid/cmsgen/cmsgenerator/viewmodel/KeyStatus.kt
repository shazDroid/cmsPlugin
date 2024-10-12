package com.shazdroid.cmsgen.cmsgenerator.viewmodel

data class KeyStatus(
    val enCount: Int,
    val arCount: Int,
    val inCmsKeyMapper: Boolean,
    var isDuplicatedInEn: Boolean,
    var isDuplicatedInAr: Boolean,
    var isMissingInEn: Boolean,
    var isMissingInAr: Boolean,
    var isMissingInCmsKeyMapper: Boolean
) {
    val inEnglishJson: Boolean
        get() = enCount > 0

    val inArabicJson: Boolean
        get() = arCount > 0
}

