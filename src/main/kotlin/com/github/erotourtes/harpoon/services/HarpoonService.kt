package com.github.erotourtes.harpoon.services

import com.github.erotourtes.harpoon.listeners.FilesRenameListener
import com.github.erotourtes.harpoon.utils.FilesFinder
import com.github.erotourtes.harpoon.utils.menu.QuickMenu
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

// TODO: optimise live save of the menu
// TODO: folding builder
// TODO: optimise live save + editor focus close trigger 2 saves
// TODO: fix rare bug with menu is overwriting itself
// TODO: fix bug with folds not closing after opening a file


@Service(Service.Level.PROJECT)
class HarpoonService(project: Project) : Disposable {
    private val menu = QuickMenu(project, this)
    private var state = State()
    private val fileEditorManager = FileEditorManager.getInstance(project)

    init {
        FilesRenameListener(::onRenameFile, this)
        syncWithMenu()
    }

    fun openMenu() {
        menu.open()
    }

    fun syncWithMenuSafe() {
        ApplicationManager.getApplication().invokeLater {
            syncWithMenu()
        }
    }

    fun syncWithMenu() {
        setPaths(menu.readLines())
    }

    fun getPaths(): List<String> = state.paths

    fun addFile(file: VirtualFile): Unit = state.add(file.path)

    /**
     * @throws Exception if file is not found or can't be opened
     */
    fun openFile(index: Int) {
        val file = getFile(index) ?: throw Exception("Can't find file")
        try {
            if (file.path == menu.virtualFile.path) menu.open()
            else fileEditorManager.openFile(file, true)
        } catch (e: Exception) {
            throw Exception("Can't find file. It might be deleted")
        }
    }

    private fun setPaths(paths: List<String>): Unit = state.set(paths)

    // TODO: needed only for test, think how to refactor
    val menuVF: VirtualFile get() = menu.virtualFile

    private fun getFile(index: Int): VirtualFile? = state.getFile(index)

    private fun onRenameFile(oldPath: String, newPath: String?) {
        val isDeleteEvent = newPath == null
        if (isDeleteEvent) state.remove(oldPath)
        else if (state.updatePathForFile(
                oldPath, newPath
            )
        ) // TODO: somehow rename listener can go crazy and spam file change events
            menu.syncWithService()
    }

    class State(
        private val filesFinder: FilesFinder = FilesFinder()
    ) {
        private var data: ArrayList<String> = ArrayList()
        private val virtualFiles = mutableMapOf<String, VirtualFile?>()

        val paths: List<String> get() = data.toList()

        fun getFile(index: Int): VirtualFile? {
            val path = data.getOrNull(index) ?: return null
            return virtualFiles.getOrPut(path) { filesFinder.findFileBy(path) }
        }

        fun set(newPaths: List<String>) {
            val filtered = newPaths.filter { it.isNotEmpty() }.distinct()
            data = ArrayList(filtered)
        }

        fun add(path: String) {
            if (data.contains(path)) return
            data.add(path)
            virtualFiles[path] = filesFinder.findFileBy(path)
        }

        fun remove(path: String) {
            val index = data.indexOf(path)
            if (index == -1) return
            data.removeAt(index)
            virtualFiles.remove(path)
        }

        fun updatePathForFile(oldPath: String, newPath: String?): Boolean {
            val index = data.indexOf(oldPath)
            if (index == -1) return false

            data[index] = newPath!!
            virtualFiles[newPath] = virtualFiles.remove(oldPath)
            return true
        }
    }

    companion object {
        fun getInstance(project: Project): HarpoonService {
            return project.service<HarpoonService>()
        }
    }

    // Needs for other classes to be able to register in Disposer
    override fun dispose() {}
}