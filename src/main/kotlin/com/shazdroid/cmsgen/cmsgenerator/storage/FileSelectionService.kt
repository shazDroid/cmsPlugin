package com.shazdroid.cmsgen.cmsgenerator.storage

import com.intellij.openapi.components.*
import com.shazdroid.cmsgen.cmsgenerator.models.FileSelectionState


@State(
    name = "FileSelectionState",
    storages = [Storage("FileSelectionState.xml")]
)
@Service
class FileSelectionService : PersistentStateComponent<FileSelectionState> {
    private var state: FileSelectionState = FileSelectionState()

    override fun getState(): FileSelectionState {
        return state
    }

    override fun loadState(state: FileSelectionState) {
        this.state = state
    }

    fun storeSelectedFiles(filePaths: List<String>) {
        state.selectedFiles = filePaths
    }

    fun getSelectedFiles(): List<String> {
        return state.selectedFiles
    }
}


