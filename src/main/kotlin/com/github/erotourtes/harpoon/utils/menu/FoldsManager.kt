package com.github.erotourtes.harpoon.utils.menu

import com.github.erotourtes.harpoon.settings.SettingsState
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.FoldingModel
import org.intellij.markdown.lexer.push

class FoldsManager(
    private val projectInfo: ProjectInfo,
    private val foldingModel: FoldingModel?,
    private var settings: FoldsSettings,
) {
    fun updateFoldsAt(line: Int, str: String) {
        if (foldingModel == null) return

        val newFolds = getFoldsFrom(line, str)
        foldingModel.runBatchFoldingOperation {
            val curLineFolds = getCurrentLineFolds(foldingModel, line, line + str.length)

            for ((start, end, placeHolder) in newFolds) {
                if (start == end) continue

                val foldAlreadyInLine = curLineFolds.find { it.startOffset == start && it.endOffset == end }
                if (foldAlreadyInLine != null) {
                    curLineFolds.remove(foldAlreadyInLine)
//                    foldAlreadyInLine.isExpanded = false
                    continue
                }

                val foldRegion =
                    foldingModel.addFoldRegion(start, end, placeHolder) ?: return@runBatchFoldingOperation
                foldRegion.isExpanded = false
            }

            // remove all folds that are not in the newFolds
            curLineFolds.forEach(foldingModel::removeFoldRegion)
        }
    }

    private fun getCurrentLineFolds(foldingModel: FoldingModel, start: Int, end: Int): MutableList<FoldRegion> {
        return foldingModel.allFoldRegions.filter {
            it.startOffset >= start && it.endOffset <= end
        }.toMutableList()
   }

    private fun getFoldsFrom(line: Int, str: String): List<Triple<Int, Int, String>> {
        val folds = ArrayList<Triple<Int, Int, String>>()
        var lastFoldIndex = 0
        if (settings.showProjectPath && str.startsWith(projectInfo.pathWithSlashAtEnd)) {
            val endIndex = projectInfo.pathWithSlashAtEnd.length
            folds.push(Triple(line, line + endIndex, projectInfo.nameWithSlashAtEnd))
            lastFoldIndex += endIndex
        }

        var count = 0
        for (index in str.length - 1 downTo lastFoldIndex) {
            if (str[index] == '/') count++
            if (count == settings.numberOfSlashes && index != lastFoldIndex) {
                val placeholder = if (str[lastFoldIndex] == '/') "/../" else ".../"
                folds.push(Triple(line + lastFoldIndex, line + index + 1, placeholder))
                break
            }
        }

        return folds
    }

    data class FoldsSettings(
        var showProjectPath: Boolean,
        var numberOfSlashes: Int,
    ) {
        companion object {
            fun fromSettings(settings: SettingsState): FoldsSettings {
                return FoldsSettings(
                    showProjectPath = settings.showProjectPath,
                    numberOfSlashes = settings.numberOfSlashes,
                )
            }
        }
    }
}