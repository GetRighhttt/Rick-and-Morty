package com.example.rickandmortymvvm.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.example.rickandmortymvvm.presentation.character.model.CharacterSaveResult
import com.example.rickandmortymvvm.support.FakeCharacterRepository
import com.example.rickandmortymvvm.support.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Checks loading retries, in-flight saves, write failures, duplicate taps, and lifecycle
 * cancellation.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()

    @After
    fun clear() {
        store.clear()
    }

    private fun viewModel(repository: FakeCharacterRepository) =
        DetailsViewModel(
                repository,
                SavedStateHandle(mapOf(DetailsViewModel.CHARACTER_ID to 42)),
            )
            .also { store.put("details", it) }

    @Test
    fun `save waits for persistence and ignores repeated taps`() = runTest {
        val repository = FakeCharacterRepository().apply { saveGate = CompletableDeferred() }
        val vm = viewModel(repository)
        runCurrent()
        assertEquals("Unknown", vm.uiState.value.character?.status)
        vm.saveCharacter()
        vm.saveCharacter()
        runCurrent()
        assertTrue(vm.uiState.value.isSaving)
        assertNull(vm.uiState.value.saveResult)
        assertEquals(1, repository.saveCalls)
        repository.saveGate!!.complete(Unit)
        runCurrent()
        assertFalse(vm.uiState.value.isSaving)
        assertEquals(CharacterSaveResult.SAVED, vm.uiState.value.saveResult)
        assertEquals("unknown", repository.savedValue?.status)
        vm.acknowledgeSaveResult()
        assertNull(vm.uiState.value.saveResult)
    }

    @Test
    fun `failed save keeps the character and allows retry`() = runTest {
        val repository = FakeCharacterRepository().apply { writeFailure = IOException() }
        val vm = viewModel(repository)
        runCurrent()
        vm.saveCharacter()
        runCurrent()
        assertFalse(vm.uiState.value.isSaving)
        assertEquals(CharacterSaveResult.FAILED, vm.uiState.value.saveResult)
        assertEquals(42, vm.uiState.value.character?.id)
        repository.writeFailure = null
        vm.saveCharacter()
        runCurrent()
        assertEquals(CharacterSaveResult.SAVED, vm.uiState.value.saveResult)
    }

    @Test
    fun `load failure can retry using the saved character ID`() = runTest {
        val repository = FakeCharacterRepository().apply { loadFailure = IOException() }
        val vm = viewModel(repository)
        runCurrent()
        assertTrue(vm.uiState.value.loadFailed)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.character)
        repository.loadFailure = null
        vm.loadCharacter()
        runCurrent()
        assertEquals(42, vm.uiState.value.character?.id)
        assertFalse(vm.uiState.value.loadFailed)
    }

    @Test
    fun `cancelling a save does not report success or failure`() = runTest {
        val repository = FakeCharacterRepository().apply { saveGate = CompletableDeferred() }
        val vm = viewModel(repository)
        runCurrent()
        vm.saveCharacter()
        runCurrent()
        store.clear()
        runCurrent()
        assertNull(vm.uiState.value.saveResult)
        assertFalse(vm.uiState.value.isSaving)
        assertNull(repository.savedValue)
    }
}
