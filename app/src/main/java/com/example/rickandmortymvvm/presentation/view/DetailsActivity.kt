package com.example.rickandmortymvvm.presentation.view

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.rickandmortymvvm.R
import com.example.rickandmortymvvm.core.util.applySystemBarsPadding
import com.example.rickandmortymvvm.core.util.setImage
import com.example.rickandmortymvvm.databinding.ActivityDetailsBinding
import com.example.rickandmortymvvm.presentation.character.model.CharacterSaveResult
import com.example.rickandmortymvvm.presentation.character.model.DetailsUiState
import com.example.rickandmortymvvm.presentation.viewmodel.DetailsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Renders profile UI state and forwards user actions; character data and save jobs belong to the
 * ViewModel.
 *
 * @author Stefan Bayne
 * @since 2023-05-05
 */
@AndroidEntryPoint
class DetailsActivity : AppCompatActivity() {
    private var _binding: ActivityDetailsBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: DetailsViewModel by viewModels()
    private var displayedImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        _binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding(applyLeft = true, applyRight = true)
        binding.toolBarLayout.applySystemBarsPadding(applyTop = true)
        binding.detailsContent.applySystemBarsPadding(applyBottom = true)
        binding.topUserAppBar.setNavigationOnClickListener { finish() }
        binding.detailsRetry.setOnClickListener { viewModel.loadCharacter() }
        binding.topUserAppBar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.add_character) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.save_character_title)
                    .setMessage(R.string.save_character_prompt)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.save) { _, _ -> viewModel.saveCharacter() }
                    .show()
                true
            } else false
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect(::render) }
        }
    }

    private fun render(state: DetailsUiState) = binding.apply {
        pbLoading.isVisible = state.isLoading || state.isSaving
        detailsContent.isVisible = state.character != null
        detailsError.isVisible = state.loadFailed
        topUserAppBar.menu.findItem(R.id.add_character).apply {
            isVisible = state.character != null
            isEnabled = !state.isSaving
        }
        state.character?.let { character ->
            if (displayedImageUrl != character.imageUrl) {
                ivImage.setImage(character.imageUrl, ivImage)
                displayedImageUrl = character.imageUrl
            }
            ivImage.contentDescription =
                getString(R.string.character_image_description, character.name)
            tvName.text = character.name
            tvSpecies.text = character.species
            tvGender.text = character.gender
            tvStatus.text = character.status
            tvCreated.text = character.firstSeenDate
        }
        state.saveResult?.let { result ->
            Snackbar.make(
                    root,
                    when (result) {
                        CharacterSaveResult.SAVED -> R.string.character_saved
                        CharacterSaveResult.FAILED -> R.string.character_save_failed
                    },
                    Snackbar.LENGTH_LONG,
                )
                .show()
            viewModel.acknowledgeSaveResult()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
