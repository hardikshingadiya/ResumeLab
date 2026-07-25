package com.example.resume.ui

import android.animation.ValueAnimator
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.resume.R
import com.example.resume.data.ResumeTextExtractor
import com.example.resume.databinding.ActivityResumeBinding
import com.example.resume.model.ResumeAnalysisResponse
import com.example.resume.ui.adapter.MissingKeywordAdapter
import com.example.resume.ui.adapter.SuggestionsAdapter
import com.example.resume.ui.viewmodel.ResumeViewModel
import kotlinx.coroutines.launch

class ResumeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResumeBinding
    private val viewModel: ResumeViewModel by viewModels()
    private val missingKeywordAdapter = MissingKeywordAdapter()
    private val suggestionsAdapter = SuggestionsAdapter()

    private var extractedResumeText: String = ""

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityResumeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ResumeTextExtractor.init(this)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setLogo(R.drawable.ic_launcher_foreground)

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
            val jobText = jobEditText.text?.toString().orEmpty()
            viewModel.analyzeResume(extractedResumeText, jobText)
        }

        uploadResumeButton.setOnClickListener {
            filePickerLauncher.launch(arrayOf(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ))
        }
    }

    private fun handleSelectedFile(uri: Uri) = with(binding) {
        val mimeType = contentResolver.getType(uri)
        val displayName = getFileName(uri)

        fileNameText.text = displayName ?: "Resume file"
        fileNameText.visibility = View.VISIBLE
        errorText.visibility = View.GONE

        // Show inline loading for extraction
        uploadResumeButton.isEnabled = false
        uploadResumeButton.text = "Extracting text..."

        lifecycleScope.launch {
            runCatching {
                ResumeTextExtractor.extractText(this@ResumeActivity, uri, mimeType)
            }.onSuccess { extractedText ->
                extractedResumeText = extractedText
                fileNameText.text = "✅ ${displayName ?: "Resume file"} — ${extractedText.length} chars extracted"
                uploadResumeButton.isEnabled = true
                uploadResumeButton.text = "Choose PDF or DOCX file"
            }.onFailure { throwable ->
                errorText.text = "Could not read file: ${throwable.message}"
                errorText.visibility = View.VISIBLE
                uploadResumeButton.isEnabled = true
                uploadResumeButton.text = "Choose PDF or DOCX file"
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else null
            } else null
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

        // Show verdict
        if (response.overallVerdict.isNotBlank()) {
            verdictText.text = response.overallVerdict
            val overallScore = (response.matchScore + response.formattingScore + response.contentScore) / 3
            scoreTotalText.text = "$overallScore%"
            verdictContainer.visibility = View.VISIBLE
        } else {
            verdictContainer.visibility = View.GONE
        }

        // Animate dashboard in
        dashboardContainer.alpha = 0f
        dashboardContainer.visibility = View.VISIBLE
        dashboardContainer.animate()
            .alpha(1f)
            .setDuration(320L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Animate scores with their specific colors
        animateScore(matchProgress, response.matchScore, getString(R.string.score_match), R.color.primary_slate)
        animateScore(formattingProgress, response.formattingScore, getString(R.string.score_format), R.color.secondary_sage)
        animateScore(contentProgress, response.contentScore, getString(R.string.score_content), R.color.accent_gold)
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
        view.setScore(0, label)
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
