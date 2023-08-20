package com.github.erotourtes.harpoon.services

import com.github.erotourtes.harpoon.utils.menu.QuickMenu
import com.github.erotourtes.harpoon.utils.XML_HARPOONER_FILE_NAME
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import kotlin.io.path.Path

@State(name = "HarpoonerState", storages = [Storage(XML_HARPOONER_FILE_NAME)])
@Service(Service.Level.PROJECT)
class HarpoonService(project: Project) : PersistentStateComponent<HarpoonService.State> {
    var menu = QuickMenu(project)
    private val virtualFiles = mutableMapOf<String, VirtualFile?>()
    private var state = State()

    fun getPaths(): List<String> = state.data.toList()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun addFile(file: VirtualFile) {
        val path = Path(file.path).toRealPath().toString()
        if (state.data.any { it == path }) return
        state.data += path
        virtualFiles[path] = file
//        menu.addToFile(path)
    }

    fun getFile(index: Int): VirtualFile? {
        val path = state.data.getOrNull(index) ?: return null
        if (path.isEmpty()) return null
        if (virtualFiles[path] == null)
            virtualFiles[path] = LocalFileSystem.getInstance().findFileByPath(path)

        return virtualFiles.getOrDefault(path, null)
    }

    fun setPaths(paths: List<String>) {
        val filtered = paths.filter { it.isNotEmpty() }.distinct()
        if (filtered != state.data)
            state.data = ArrayList(filtered)
    }

    class State {
        var data: ArrayList<String> = ArrayList()
    }

    companion object {
        fun getInstance(project: Project): HarpoonService {
//            return project.getService(HarpoonService::class.java)
            return project.service<HarpoonService>()
        }
    }
}