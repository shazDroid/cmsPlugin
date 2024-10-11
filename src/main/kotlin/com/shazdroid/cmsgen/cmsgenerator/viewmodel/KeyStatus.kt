package com.shazdroid.cmsgen.cmsgenerator.viewmodel

data class KeyStatus(
    val enCount: Int,
    val arCount: Int,
    val inCmsKeyMapper: Boolean,
    val isDuplicatedInEn: Boolean,
    val isDuplicatedInAr: Boolean,
    val isMissingInEn: Boolean,
    val isMissingInAr: Boolean,
    val isMissingInCmsKeyMapper: Boolean
) {
    val inEnglishJson: Boolean
        get() = enCount > 0

    val inArabicJson: Boolean
        get() = arCount > 0
}

