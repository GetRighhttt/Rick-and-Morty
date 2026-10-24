package com.example.rickandmortymvvm.presentation.view

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.rickandmortymvvm.R
import com.example.rickandmortymvvm.core.util.setImage
import com.example.rickandmortymvvm.databinding.RmListItemBinding
import com.example.rickandmortymvvm.presentation.character.model.CharacterUiModel

/**
 * Binds paged characters to reusable cards and forwards profile selections.
 *
 * @author Stefan Bayne
 * @since 2023-05-05
 */
class RickAndMortyAdapter(private val context: Context) :
    PagingDataAdapter<CharacterUiModel, RickAndMortyAdapter.ViewHolder>(diffCallback) {

    companion object {

        private val diffCallback =
            object : DiffUtil.ItemCallback<CharacterUiModel>() {

                override fun areItemsTheSame(
                    oldItem: CharacterUiModel,
                    newItem: CharacterUiModel,
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: CharacterUiModel,
                    newItem: CharacterUiModel,
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }

    inner class ViewHolder(private val binding: RmListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(rmUser: CharacterUiModel) {
            binding.apply {
                rmFullName.text = rmUser.name
                rmStatus.text = rmUser.status
                rmSpecies.text =
                    context.getString(
                        R.string.character_summary,
                        rmUser.species,
                        rmUser.gender,
                    )
                rmImage.contentDescription =
                    context.getString(
                        R.string.character_image_description,
                        rmUser.name,
                    )

                // set image with Glide extension method
                rmImage.setImage(rmUser.imageUrl, rmImage)
                root.setOnClickListener {
                    onItemClickListener?.let {
                        it(rmUser)
                    }
                }
            }
        }
    }

    /*
    Item click listener variable.
    */
    private var onItemClickListener: ((CharacterUiModel) -> Unit)? = null

    /*
    Setter method for the onItemClickListener.
    */
    fun setOnItemClickListener(listener: ((CharacterUiModel) -> Unit)?) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RmListItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = getItem(position)
        if (currentItem != null) {
            holder.bind(currentItem)
        } else {
            return
        }
    }
}
