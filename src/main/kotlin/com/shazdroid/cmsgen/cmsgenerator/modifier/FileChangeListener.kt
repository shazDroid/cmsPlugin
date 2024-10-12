package com.shazdroid.cmsgen.cmsgenerator.modifier

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener

class FileChangeListener(
    private val project: Project,
    private val onFileChanged: () -> Unit
) : VirtualFileListener {

    override fun contentsChanged(event: VirtualFileEvent) {
        val fileName = event.file.name
        if (fileName == "DefaultEn.json" || fileName == "DefaultAr.json") {
            onFileChanged.invoke()
        }
    }
}
