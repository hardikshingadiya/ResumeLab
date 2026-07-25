package com.example.resume.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.resume.BuildConfig
import com.example.resume.data.ResumeAnalysisRepository
import com.example.resume.model.ResumeAnalysisResponse
import com.example.resume.ui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResumeViewModel(
    private val repository: ResumeAnalysisRepository = ResumeAnalysisRepository()
) : ViewModel() {

    private val _analysisState = MutableLiveData<UiState<ResumeAnalysisResponse>>(UiState.Idle)
    val analysisState: LiveData<UiState<ResumeAnalysisResponse>> = _analysisState

    fun analyzeResume(resumeText: String, jobDescriptionText: String) {
        if (resumeText.isBlank() || jobDescriptionText.isBlank()) {
            _analysisState.value = UiState.Error("Add both resume text and a job description before analyzing.")
            return
        }

        _analysisState.value = UiState.Loading
        viewModelScope.launch {
            runCatching {
                val apiKey = BuildConfig.GENAI_API_KEY.orEmpty()
                val rawResponse = withContext(Dispatchers.IO) {
                    if (apiKey.isBlank()) {
                        null
                    } else {
                        repository.analyzeWithGemini(apiKey, resumeText, jobDescriptionText)
                    }
                }
                withContext(Dispatchers.Default) {
                    rawResponse?.let(repository::parseAnalysis)
                        ?: repository.fallbackAnalysis(resumeText, jobDescriptionText)
                }
            }.onSuccess { response ->
                _analysisState.value = UiState.Success(response)
            }.onFailure { throwable ->
                val fallback = withContext(Dispatchers.Default) {
                    repository.fallbackAnalysis(resumeText, jobDescriptionText)
                }
                if (fallback.missingKeywords.isNotEmpty()) {
                    _analysisState.value = UiState.Success(fallback)
                } else {
                    _analysisState.value = UiState.Error(throwable.message ?: "Unable to analyze this resume right now.")
                }
            }
        }
    }
}
