package com.example.resume.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.resume.databinding.ItemMissingKeywordBinding
import com.example.resume.model.MissingKeyword

class MissingKeywordAdapter : RecyclerView.Adapter<MissingKeywordAdapter.KeywordViewHolder>() {

    private val items = mutableListOf<MissingKeyword>()

    fun submitList(keywords: List<MissingKeyword>) {
        items.clear()
        items.addAll(keywords)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeywordViewHolder {
        val binding = ItemMissingKeywordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KeywordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KeywordViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class KeywordViewHolder(
        private val binding: ItemMissingKeywordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(keyword: MissingKeyword) = with(binding) {
            keywordText.text = keyword.keyword
            impactText.text = "+${keyword.impactPercent}%"
            reasonText.text = keyword.reason
        }
    }
}
