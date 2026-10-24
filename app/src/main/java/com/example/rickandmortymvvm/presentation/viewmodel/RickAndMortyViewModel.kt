package com.example.rickandmortymvvm.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.rickandmortymvvm.domain.character.repository.CharacterRepository
import com.example.rickandmortymvvm.presentation.character.mapper.toDomain
import com.example.rickandmortymvvm.presentation.character.mapper.toUiModel
import com.example.rickandmortymvvm.presentation.character.model.CharacterFilters
import com.example.rickandmortymvvm.presentation.character.model.CharacterGender
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Owns restored browser inputs, cancels obsolete paging streams, and publishes UI characters.
 *
 * @author Stefan Bayne
 * @since 2023-05-01
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class RickAndMortyViewModel @Inject constructor(
    private val repository: CharacterRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val filters: StateFlow<CharacterFilters> =
        savedStateHandle.getStateFlow(FILTERS_KEY, CharacterFilters())

    // Cache the stream after changing Pagers and mapping domain values for the UI.
    val characters =
        filters
            .flatMapLatest { filters ->
                repository.searchCharacters(filters.query, filters.gender.toDomain())
            }
            .map { page -> page.map { it.toUiModel() } }
            .cachedIn(viewModelScope)

    fun searchCharacters(query: String) = updateFilters { it.copy(query = query.trim()) }

    fun filterByGender(gender: CharacterGender) = updateFilters { it.copy(gender = gender) }

    fun showAllCharacters() {
        savedStateHandle[FILTERS_KEY] = CharacterFilters()
    }

    private fun updateFilters(transform: (CharacterFilters) -> CharacterFilters) {
        savedStateHandle[FILTERS_KEY] = transform(filters.value)
    }

    companion object {
        private const val FILTERS_KEY = "character_filters"
    }
}
