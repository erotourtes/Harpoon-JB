package com.github.erotourtes.harpoon.services

import com.intellij.openapi.vfs.VirtualFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class HarpoonServiceStateTest {
    private lateinit var filesFinder: HarpoonService.FilesFinder
    private val path1 = "/project/files/1.txt"
    private val path2 = "/project/files/2.txt"
    private lateinit var state: HarpoonService.State
    private lateinit var vf1: VirtualFile
    private lateinit var vf2: VirtualFile

    @BeforeEach
    fun setUp() {
        filesFinder = mock(HarpoonService.FilesFinder::class.java)
        state = HarpoonService.State(filesFinder)

        vf1 = mock(VirtualFile::class.java)
        vf2 = mock(VirtualFile::class.java)
        `when`(filesFinder.findFileBy(path1)).thenReturn(vf1)
        `when`(filesFinder.findFileBy(path2)).thenReturn(vf2)
    }


    @Test
    fun `should add path to state`() {
        state.add(path1)

        assert(state.paths.contains(path1))
        assert(state.getFile(0) == vf1)
    }

    @Test
    fun `should add several paths to state`() {
        state.add(path1)
        state.add(path2)

        assert(state.paths[0] == path1)
        assert(state.paths[1] == path2)
        assert(state.getFile(0) == vf1)
        assert(state.getFile(1) == vf2)
    }

    @Test
    fun `should remove path from state`() {
        state.add(path1)
        state.remove(path1)

        assert(state.paths.isEmpty())
    }

    @Test
    fun `should not remove non-existing path`() {
        state.add(path1)
        state.remove(path2)

        assert(state.paths[0] == path1)
    }

    @Test
    fun `should update path in state`() {
        state.add(path1)
        state.updatePathForFile(path1, path2)

        assert(state.paths[0] == path2)
        assert(state.getFile(0) == vf1)
    }

    @Test
    fun `should not update non-existing path`() {
        state.add(path1)
        state.updatePathForFile(path2, path1)

        assert(state.paths[0] == path1)
    }
}