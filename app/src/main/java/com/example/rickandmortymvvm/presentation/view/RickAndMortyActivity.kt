package com.example.rickandmortymvvm.presentation.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.example.rickandmortymvvm.R
import com.example.rickandmortymvvm.core.util.applySystemBarsPadding
import com.example.rickandmortymvvm.core.util.createSnackBar
import com.example.rickandmortymvvm.databinding.ActivityRickAndMortyBinding
import com.example.rickandmortymvvm.presentation.character.model.CharacterFilters
import com.example.rickandmortymvvm.presentation.character.model.CharacterGender
import com.example.rickandmortymvvm.presentation.character.model.CharacterUiModel
import com.example.rickandmortymvvm.presentation.viewmodel.DetailsViewModel
import com.example.rickandmortymvvm.presentation.viewmodel.RickAndMortyViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Renders the paged character directory and keeps search and filter controls in sync with the
 * ViewModel.
 *
 * @author Stefan Bayne
 * @since 2023-05-01
 */
@AndroidEntryPoint
class RickAndMortyActivity : AppCompatActivity() {

    private var _binding: ActivityRickAndMortyBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: RickAndMortyViewModel by viewModels()
    private lateinit var rmAdapter: RickAndMortyAdapter
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        _binding = ActivityRickAndMortyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mainContent.applySystemBarsPadding(applyLeft = true, applyRight = true)
        binding.toolBarLayout.applySystemBarsPadding(applyTop = true)
        binding.rvRmList.applySystemBarsPadding(applyBottom = true)
        binding.navView.applySystemBarsPadding(
            applyLeft = true,
            applyTop = true,
            applyRight = true,
            applyBottom = true,
        )

        binding.apply {
            setSupportActionBar(topUserAppBar)
            toggle =
                ActionBarDrawerToggle(
                    this@RickAndMortyActivity,
                    drawerLayout,
                    topUserAppBar,
                    R.string.open,
                    R.string.close,
                )
            drawerLayout.addDrawerListener(toggle)
            toggle.syncState()

            setNavigationDrawer()
        }

        updateScreenState()
    }

    private fun updateScreenState() {
        initRecyclerViewAndLoadStateAdapter()
        setupSearchView()
        setupFilters()
        observeScreenState()
    }

    private fun setNavigationDrawer() = binding.apply {
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_account -> {
                    rvRmList.smoothScrollToPosition(0)
                    rmSearchView.clearFocus()
                    createSnackBar("Loading Website...", binding.root)

                    // navigate to Rick and Morty website
                    val uriString = "https://www.adultswim.com/videos/rick-and-morty/rick-and-morty"
                    val intent =
                        Intent(Intent.ACTION_VIEW).apply {
                            data = uriString.toUri()
                        }
                    try { // error handling
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                    drawerLayout.close()
                }

                R.id.nav_share -> {
                    rvRmList.smoothScrollToPosition(0)
                    rmSearchView.clearFocus()
                    createSnackBar("Loading...", binding.root)

                    // share an email about the application
                    val intent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_EMAIL,
                                arrayOf("stefanbayne@gmail.com"),
                            )
                            putExtra(Intent.EXTRA_SUBJECT, "Sharing application")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Please checkout my Rick and Morty application that I have created!",
                            )
                        }
                    // another approach to error handling with resolve activity
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                    drawerLayout.close()
                }

                R.id.nav_youtube -> {
                    rvRmList.smoothScrollToPosition(0)
                    rmSearchView.clearFocus()

                    val youtubeString =
                        "https://www.youtube.com/results?search_query=rick+and+morty"
                    val intent =
                        Intent(Intent.ACTION_VIEW).apply {
                            data = youtubeString.toUri()
                        }
                    try { // error handling
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }

                    drawerLayout.close()
                }

                R.id.nav_list -> {
                    rvRmList.smoothScrollToPosition(0)
                    rmSearchView.clearFocus()
                    val savedIntent = Intent(this@RickAndMortyActivity, SavedActivity::class.java)
                    startActivity(savedIntent)
                    drawerLayout.close()
                }

                R.id.nav_home -> {
                    rvRmList.smoothScrollToPosition(0)
                    viewModel.showAllCharacters()
                    createSnackBar("Going Home", binding.root)
                    drawerLayout.close()
                }
            }
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.browser_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.open_collection) {
            startActivity(Intent(this, SavedActivity::class.java))
            return true
        }
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun observeScreenState() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                viewModel.characters.collectLatest { pagingData ->
                    rmAdapter.submitData(pagingData)
                }
            }
            launch {
                rmAdapter.loadStateFlow.collectLatest(::renderLoadState)
            }
            launch {
                viewModel.filters.collectLatest(::renderFilters)
            }
        }
    }

    private fun initRecyclerViewAndLoadStateAdapter() {
        binding.rvRmList.apply {
            rmAdapter = RickAndMortyAdapter(this@RickAndMortyActivity)
            val header = RMLoadStateAdapter { rmAdapter.retry() }
            adapter =
                rmAdapter.withLoadStateHeaderAndFooter(
                    header = header,
                    footer = RMLoadStateAdapter { rmAdapter.retry() },
                )
            layoutManager =
                GridLayoutManager(
                        this@RickAndMortyActivity,
                        if (resources.configuration.fontScale >= 1.3f) 1
                        else resources.getInteger(R.integer.character_grid_columns),
                    )
                    .apply {
                        // Paging progress and retry rows span the whole grid.
                        spanSizeLookup =
                            object : GridLayoutManager.SpanSizeLookup() {
                                override fun getSpanSize(position: Int): Int {
                                    val characterPositions =
                                        header.itemCount until
                                            (header.itemCount + rmAdapter.itemCount)
                                    return if (position in characterPositions) 1 else spanCount
                                }
                            }
                    }
            setHasFixedSize(true)
        }
        rmAdapter.setOnItemClickListener(::openDetails)
        binding.refreshRetry.setOnClickListener { rmAdapter.retry() }
    }

    private fun setupSearchView() =
        binding.rmSearchView.apply {
            setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {

                        if (query != null) {
                            binding.rvRmList.smoothScrollToPosition(0)
                            viewModel.searchCharacters(query)
                            clearFocus()
                        }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        if (newText.isNullOrEmpty() && viewModel.filters.value.query.isNotEmpty()) {
                            viewModel.searchCharacters("")
                        }
                        return true
                    }
                }
            )
        }

    private fun renderLoadState(loadStates: CombinedLoadStates) = binding.apply {
        val refresh = loadStates.refresh
        val hasItems = rmAdapter.itemCount > 0
        val showEmptyState =
            refresh is LoadState.NotLoading && loadStates.append.endOfPaginationReached && !hasItems
        val showErrorState = refresh is LoadState.Error

        pbRm.isVisible = refresh is LoadState.Loading
        rvRmList.isVisible = hasItems && !showErrorState
        refreshState.isVisible = showEmptyState || showErrorState
        refreshRetry.isVisible = showErrorState

        refreshMessage.setText(
            if (showErrorState) R.string.characters_failed_to_load else R.string.no_characters_found
        )
    }

    private fun setupFilters() {
        binding.genderFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val gender =
                when (checkedIds.firstOrNull()) {
                    R.id.filter_male -> CharacterGender.MALE
                    R.id.filter_female -> CharacterGender.FEMALE
                    else -> CharacterGender.ALL
                }
            if (gender != viewModel.filters.value.gender) {
                binding.rvRmList.scrollToPosition(0)
                viewModel.filterByGender(gender)
            }
        }
    }

    private fun renderFilters(filters: CharacterFilters) = binding.apply {
        if (rmSearchView.query.toString() != filters.query) {
            rmSearchView.setQuery(filters.query, false)
        }
        genderFilters.check(
            when (filters.gender) {
                CharacterGender.MALE -> R.id.filter_male
                CharacterGender.FEMALE -> R.id.filter_female
                CharacterGender.ALL -> R.id.filter_all
            }
        )
    }

    private fun openDetails(character: CharacterUiModel) {
        val detailIntent =
            Intent(this, DetailsActivity::class.java)
                .putExtra(DetailsViewModel.CHARACTER_ID, character.id)
        startActivity(detailIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
