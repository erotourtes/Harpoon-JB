package com.github.erotourtes.harpoon.utils

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

class FilesFinder(
    private val localFileSystem: LocalFileSystem = LocalFileSystem.getInstance()
) {
    fun findFileBy(path: String): VirtualFile? = localFileSystem.findFileByPath(path)
}

