package com.shazdroid.cmsgen.cmsgenerator.cms_media_keys

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@State(name = "AssetsDirectorySettings", storages = [Storage("AssetsDirectorySettings.xml")])
class AssetsDirectorySettings(private val project: Project) : PersistentStateComponent<AssetsDirectorySettings.State> {

    data class State(var assetsDirectoryPath: String = "")

    private var state = State()

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        this.state = state
    }

    var assetsDirectoryPath: String
        get() = state.assetsDirectoryPath
        set(value) {
            state.assetsDirectoryPath = value
        }

    companion object {
        fun getInstance(project: Project): AssetsDirectorySettings {
            return project.getService(AssetsDirectorySettings::class.java)
        }
    }
}
