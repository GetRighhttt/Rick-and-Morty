package com.example.rickandmortymvvm.presentation.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.rickandmortymvvm.R
import com.example.rickandmortymvvm.core.util.applySystemBarsPadding
import com.example.rickandmortymvvm.databinding.ActivitySavedBinding
import com.example.rickandmortymvvm.presentation.character.model.SavedCharactersError
import com.example.rickandmortymvvm.presentation.character.model.SavedCharactersUiState
import com.example.rickandmortymvvm.presentation.viewmodel.DetailsViewModel
import com.example.rickandmortymvvm.presentation.viewmodel.SavedViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Renders collection UI state and sends ID-based actions; the ViewModel owns deleted data and undo.
 *
 * @author Stefan Bayne
 * @since 2023-05-22
 */
@AndroidEntryPoint
class SavedActivity : AppCompatActivity() {
    private var _binding: ActivitySavedBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var scAdapter: SavedCharactersAdapter
    private val viewModel: SavedViewModel by viewModels()
    private var swipedCharacterId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        _binding = ActivitySavedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding(applyLeft = true, applyRight = true)
        binding.toolBarLayout.applySystemBarsPadding(applyTop = true)
        binding.rvSavedCharacters.applySystemBarsPadding(applyBottom = true)
        getSharedPreferences("sharedPrefs", MODE_PRIVATE)
            .getString(LoginActivity.LOGIN, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { name ->
                binding.topUserAppBar.title = getString(R.string.named_collection, name)
            }
        scAdapter = SavedCharactersAdapter(this)
        binding.rvSavedCharacters.apply {
            adapter = scAdapter
            layoutManager =
                GridLayoutManager(
                    this@SavedActivity,
                    if (resources.configuration.fontScale >= 1.3f) 1
                    else resources.getInteger(R.integer.character_grid_columns),
                )
            setHasFixedSize(true)
        }
        scAdapter.setOnItemClickListener { character ->
            startActivity(
                Intent(this, DetailsActivity::class.java)
                    .putExtra(DetailsViewModel.CHARACTER_ID, character.id)
            )
        }
        binding.topUserAppBar.setNavigationOnClickListener { finish() }
        binding.savedRetry.setOnClickListener { viewModel.observeCharacters() }
        binding.topUserAppBar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.delete_character) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.clear_collection_title)
                    .setMessage(R.string.clear_collection_prompt)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteAllCharacters() }
                    .show()
                true
            } else false
        }
        setupSwipeToRemove()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect(::render) }
        }
    }

    private fun render(state: SavedCharactersUiState) = binding.apply {
        scAdapter.differ.submitList(state.characters)
        if (state.characters.none { it.id == swipedCharacterId }) swipedCharacterId = null
        pbSaved.isVisible = state.isLoading || state.isUpdating
        val loadFailed = state.error == SavedCharactersError.LOAD
        rvSavedCharacters.isVisible =
            !state.isLoading && state.characters.isNotEmpty() && !loadFailed
        savedEmptyState.isVisible = !state.isLoading && (state.characters.isEmpty() || loadFailed)
        savedEmptyTitle.setText(
            if (loadFailed) R.string.saved_load_failed else R.string.saved_empty_title
        )
        savedEmptyMessage.isVisible = !loadFailed
        savedRetry.isVisible = loadFailed
        topUserAppBar.menu.findItem(R.id.delete_character).isEnabled =
            state.characters.isNotEmpty() && !state.isUpdating
        state.removal?.let { removal ->
            Snackbar.make(
                    root,
                    getString(R.string.character_removed, removal.name),
                    Snackbar.LENGTH_LONG,
                )
                .setAction(R.string.undo) { viewModel.undoDelete(removal.id) }
                .show()
            viewModel.acknowledgeRemoval()
        }
        state.error
            ?.takeUnless { it == SavedCharactersError.LOAD }
            ?.let {
                Snackbar.make(root, R.string.collection_update_failed, Snackbar.LENGTH_LONG).show()
                viewModel.acknowledgeError()
                // Room emits no new list after a failed delete, so restore the swiped card
                // explicitly.
                swipedCharacterId?.let { id ->
                    val position = scAdapter.differ.currentList.indexOfFirst { it.id == id }
                    if (position >= 0) scAdapter.notifyItemChanged(position)
                }
                swipedCharacterId = null
            }
    }

    private fun setupSwipeToRemove() {
        val callback =
            object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun getSwipeDirs(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                ): Int =
                    if (viewModel.uiState.value.isUpdating) 0
                    else super.getSwipeDirs(recyclerView, viewHolder)

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder,
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.bindingAdapterPosition
                    val character = scAdapter.differ.currentList.getOrNull(position) ?: return
                    if (viewModel.uiState.value.isUpdating) {
                        scAdapter.notifyItemChanged(position)
                        return
                    }
                    swipedCharacterId = character.id
                    viewModel.deleteCharacter(character.id)
                }
            }
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvSavedCharacters)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
