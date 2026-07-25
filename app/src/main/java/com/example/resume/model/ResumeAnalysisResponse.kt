package com.example.resume.model

data class ResumeAnalysisResponse(
    val matchScore: Int,
    val formattingScore: Int,
    val contentScore: Int,
    val missingKeywords: List<MissingKeyword>,
    val improvedSuggestions: List<ImprovedSuggestion>,
    val overallVerdict: String = ""
)

data class MissingKeyword(
    val keyword: String,
    val impactPercent: Int,
    val reason: String
)

data class ImprovedSuggestion(
    val originalText: String,
    val suggestedRewrite: String,
    val explanation: String,
    val isOptional: Boolean = false
)
