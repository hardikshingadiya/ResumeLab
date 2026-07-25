package com.example.resume.ui

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.resume.R
import com.example.resume.databinding.ActivityResumeBinding
import com.example.resume.model.ResumeAnalysisResponse
import com.example.resume.ui.adapter.MissingKeywordAdapter
import com.example.resume.ui.adapter.SuggestionsAdapter
import com.example.resume.ui.viewmodel.ResumeViewModel

class ResumeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResumeBinding
    private val viewModel: ResumeViewModel by viewModels()
    private val missingKeywordAdapter = MissingKeywordAdapter()
    private val suggestionsAdapter = SuggestionsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityResumeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()
        setupLists()
        setupActions()
        observeState()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun setupLists() = with(binding) {
        missingKeywordsRecycler.layoutManager = LinearLayoutManager(this@ResumeActivity)
        missingKeywordsRecycler.adapter = missingKeywordAdapter
        suggestionsRecycler.layoutManager = LinearLayoutManager(this@ResumeActivity)
        suggestionsRecycler.adapter = suggestionsAdapter
    }

    private fun setupActions() = with(binding) {
        analyzeButton.setOnClickListener {
            viewModel.analyzeResume(
                resumeEditText.text?.toString().orEmpty(),
                jobEditText.text?.toString().orEmpty()
            )
        }
    }

    private fun observeState() {
        viewModel.analysisState.observe(this) { state ->
            when (state) {
                UiState.Idle -> Unit
                UiState.Loading -> showLoading()
                is UiState.Success -> showSuccess(state.data)
                is UiState.Error -> showError(state.message)
            }
        }
    }

    private fun showLoading() = with(binding) {
        errorText.visibility = View.GONE
        loadingContainer.visibility = View.VISIBLE
        dashboardContainer.visibility = View.GONE
        analyzeButton.isEnabled = false
        analyzeButton.text = "Working..."
    }

    private fun showSuccess(response: ResumeAnalysisResponse) = with(binding) {
        loadingContainer.visibility = View.GONE
        errorText.visibility = View.GONE
        analyzeButton.isEnabled = true
        analyzeButton.text = getString(R.string.analyze_resume)
        missingKeywordAdapter.submitList(response.missingKeywords)
        suggestionsAdapter.submitList(response.improvedSuggestions)
        dashboardContainer.alpha = 0f
        dashboardContainer.visibility = View.VISIBLE
        dashboardContainer.animate()
            .alpha(1f)
            .setDuration(320L)
            .setInterpolator(DecelerateInterpolator())
            .start()
        animateScore(matchProgress, response.matchScore, "Match", R.color.primary_slate)
        animateScore(formattingProgress, response.formattingScore, "Format", R.color.text_secondary)
        animateScore(contentProgress, response.contentScore, "Content", R.color.secondary_sage)
    }

    private fun showError(message: String) = with(binding) {
        loadingContainer.visibility = View.GONE
        errorText.text = message
        errorText.alpha = 0f
        errorText.visibility = View.VISIBLE
        errorText.animate().alpha(1f).setDuration(220L).start()
        analyzeButton.isEnabled = true
        analyzeButton.text = getString(R.string.analyze_resume)
    }

    private fun animateScore(
        view: com.example.resume.ui.widget.RoundProgressView,
        target: Int,
        label: String,
        colorRes: Int
    ) {
        view.setAccentColor(ContextCompat.getColor(this, colorRes))
        ValueAnimator.ofInt(0, target.coerceIn(0, 100)).apply {
            duration = 900L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                view.setScore(animator.animatedValue as Int, label)
            }
            start()
        }
    }
}
