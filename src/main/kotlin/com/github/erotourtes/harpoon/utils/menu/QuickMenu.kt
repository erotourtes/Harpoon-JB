package com.github.erotourtes.harpoon.utils.menu

import com.github.erotourtes.harpoon.listeners.FileEditorListener
import com.github.erotourtes.harpoon.services.HarpoonService
import com.github.erotourtes.harpoon.services.settings.SettingsState
import com.github.erotourtes.harpoon.utils.IDEA_PROJECT_FOLDER
import com.github.erotourtes.harpoon.utils.MENU_NAME
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.MessageBusConnection
import java.io.File


// TODO: think about settings encapsulation
class QuickMenu(private val project: Project) {
    val projectInfo: ProjectInfo
    private lateinit var menuFile: File
    lateinit var virtualFile: VirtualFile
        private set
    private var connection: MessageBusConnection? = null
    private val foldManager: FoldManager
    private var processor: PathsProcessor

    init {
        initMenuFile()
        projectInfo = ProjectInfo.from(virtualFile.path)
        SettingsState.getInstance().addObserver { updateSettings(it) }

        foldManager = FoldManager(this, project)
        processor = PathsProcessor(projectInfo)
    }

    fun readLines(): List<String> {
        val docManager = FileDocumentManager.getInstance()
        val document = docManager.getDocument(virtualFile) ?: throw Error("Can't read file")

        return document.text.split("\n").map { processor.unprocess(it) }
    }

    fun isMenuFile(path: String): Boolean = path == menuFile.path

    fun connectListener(): QuickMenu {
        if (connection != null) return this
        connection = ApplicationManager.getApplication().messageBus.connect()
        connection!!.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER, FileEditorListener()
        )

        return this
    }

    fun disconnectListener(): QuickMenu {
        connection?.disconnect()
        connection = null

        return this
    }

    fun open(): QuickMenu {
        val fileManager = FileEditorManager.getInstance(project)
        val harpoonService = HarpoonService.getInstance(project)

        if (!virtualFile.isValid)
            initMenuFile()

        fileManager.openFile(virtualFile, true)
        updateFile(harpoonService.getPaths())
        foldManager.collapseAllFolds()
        setCursorToEnd()

        return this
    }

    private fun updateFile(content: List<String>): QuickMenu {
        ApplicationManager.getApplication().runWriteAction {
            val docManager = FileDocumentManager.getInstance()
            val document = docManager.getDocument(virtualFile) ?: return@runWriteAction

            val processedContent = processor.process(content)
            processedContent.joinToString("\n").let { document.setText(it) }

            processedContent.forEachIndexed { index, it ->
                val line = document.getLineStartOffset(index)
                foldManager.addFoldsToLine(line, it)
            }
        }

        return this
    }

    private fun updateSettings(settings: SettingsState) {
        foldManager.updateSettings(settings)

        processor.updateSettings(settings)
        val harpoonService = HarpoonService.getInstance(project)
        updateFile(harpoonService.getPaths())
    }

    private fun setCursorToEnd() {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val caretModel = editor.caretModel
        val currentLineNumber = caretModel.logicalPosition.line
        val currentLineEndOffset = editor.document.getLineEndOffset(currentLineNumber)
        caretModel.moveToOffset(currentLineEndOffset)
    }

    fun isMenuFileOpenedWithCurEditor(): Boolean {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return false
        return isMenuFileOpenedWith(editor)
    }

    private fun isMenuFileOpenedWith(editor: Editor): Boolean {
        val editorFilePath = FileDocumentManager.getInstance().getFile(editor.document)?.path ?: return false
        return editorFilePath == virtualFile.path
    }

    private fun initMenuFile() {
        menuFile = getMenuFile()
        virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(menuFile)
            ?: throw Exception("File is not found, this should not happen")
    }

    private fun getMenuFile(): File {
        val tmpProjectInfo = ProjectInfo.from(project.projectFilePath)
        if (tmpProjectInfo.pathWithSlashAtEnd.isEmpty()) return File.createTempFile(MENU_NAME, null)

        val projectPath = tmpProjectInfo.pathWithSlashAtEnd + IDEA_PROJECT_FOLDER
        val menuPath = projectPath.plus("/$MENU_NAME")

        val menu = File(menuPath)
        menu.createNewFile() // create file if it doesn't exist
        return menu
    }
}