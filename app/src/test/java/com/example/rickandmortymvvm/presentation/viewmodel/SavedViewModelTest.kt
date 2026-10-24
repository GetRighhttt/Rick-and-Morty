package com.example.rickandmortymvvm.presentation.viewmodel

import androidx.lifecycle.ViewModelStore
import com.example.rickandmortymvvm.presentation.character.model.SavedCharactersError
import com.example.rickandmortymvvm.support.FakeCharacterRepository
import com.example.rickandmortymvvm.support.MainDispatcherRule
import com.example.rickandmortymvvm.support.characterFixture
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Verifies collection failure state and that undo retains original domain fields instead of
 * formatted UI values.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()

    @After
    fun clear() {
        store.clear()
    }

    private fun viewModel(repository: FakeCharacterRepository) =
        SavedViewModel(repository).also { store.put("saved", it) }

    @Test
    fun `undo saves the original domain character rather than display values`() = runTest {
        val original = characterFixture()
        val repository = FakeCharacterRepository().apply { saved.value = listOf(original) }
        val vm = viewModel(repository)
        assertTrue(vm.uiState.value.isLoading)
        runCurrent()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Unknown", vm.uiState.value.characters.single().status)
        vm.deleteCharacter(original.id)
        runCurrent()
        assertTrue(vm.uiState.value.characters.isEmpty())
        assertEquals(original.name, vm.uiState.value.removal?.name)
        vm.acknowledgeRemoval()
        assertNull(vm.uiState.value.removal?.name)
        vm.undoDelete(42)
        runCurrent()
        assertEquals(original, repository.savedValue)
        assertEquals(original.type, repository.savedValue?.type)
        assertEquals(original.url, repository.savedValue?.url)
        assertEquals("unknown", repository.savedValue?.status)
        assertEquals(original.id, vm.uiState.value.characters.single().id)
    }

    @Test
    fun `failed removal leaves content available and clears progress`() = runTest {
        val repository =
            FakeCharacterRepository().apply {
                saved.value = listOf(characterFixture())
                writeFailure = IOException()
            }
        val vm = viewModel(repository)
        runCurrent()
        vm.deleteCharacter(42)
        runCurrent()
        assertEquals(1, vm.uiState.value.characters.size)
        assertEquals(SavedCharactersError.DELETE, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isUpdating)
        assertNull(vm.uiState.value.removal?.name)
    }

    @Test
    fun `an old undo action cannot restore a newer deletion`() = runTest {
        val repository =
            FakeCharacterRepository().apply {
                saved.value = listOf(characterFixture(42), characterFixture(43))
            }
        val vm = viewModel(repository)
        runCurrent()
        vm.deleteCharacter(42)
        runCurrent()
        vm.deleteCharacter(43)
        runCurrent()
        vm.undoDelete(42)
        runCurrent()
        assertTrue(repository.saved.value.isEmpty())
        assertEquals(0, repository.saveCalls)
        vm.undoDelete(43)
        runCurrent()
        assertEquals(43, repository.saved.value.single().id)
    }

    @Test
    fun `clearing the collection invalidates the previous undo`() = runTest {
        val repository =
            FakeCharacterRepository().apply { saved.value = listOf(characterFixture()) }
        val vm = viewModel(repository)
        runCurrent()
        vm.deleteCharacter(42)
        runCurrent()
        vm.deleteAllCharacters()
        runCurrent()
        vm.undoDelete(42)
        runCurrent()
        assertTrue(repository.saved.value.isEmpty())
        assertEquals(0, repository.saveCalls)
    }
}
