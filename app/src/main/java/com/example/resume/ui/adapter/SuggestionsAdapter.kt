package com.example.resume.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.resume.databinding.ItemSuggestionBinding
import com.example.resume.model.ImprovedSuggestion

class SuggestionsAdapter : RecyclerView.Adapter<SuggestionsAdapter.SuggestionViewHolder>() {

    private val items = mutableListOf<ImprovedSuggestion>()

    fun submitList(suggestions: List<ImprovedSuggestion>) {
        items.clear()
        items.addAll(suggestions)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val binding = ItemSuggestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SuggestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class SuggestionViewHolder(
        private val binding: ItemSuggestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(suggestion: ImprovedSuggestion) = with(binding) {
            originalText.text = suggestion.originalText
            suggestedText.text = suggestion.suggestedRewrite
            explanationText.text = suggestion.explanation
        }
    }
}
