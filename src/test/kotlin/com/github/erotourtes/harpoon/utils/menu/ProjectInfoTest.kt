package com.github.erotourtes.harpoon.utils.menu

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProjectInfoTest {
    @Test
    fun from() {
        val path = "/home/user/Projects/MyProject/.idea/Harpooner Menu"
        val projectInfo = ProjectInfo.from(path)

        assertEquals("MyProject/", projectInfo.nameWithSlashAtEnd)
        assertEquals("/home/user/Projects/MyProject/", projectInfo.pathWithSlashAtEnd)
    }

    @Test
    fun fromEmpty() {
        val path = ""
        val projectInfo = ProjectInfo.from(path)

        assertEquals("", projectInfo.nameWithSlashAtEnd)
        assertEquals("", projectInfo.pathWithSlashAtEnd)
    }
}