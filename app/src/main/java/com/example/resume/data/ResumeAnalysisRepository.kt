package com.example.resume.data

import com.example.resume.model.ImprovedSuggestion
import com.example.resume.model.MissingKeyword
import com.example.resume.model.ResumeAnalysisResponse
import com.google.genai.kotlin.Client
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

class ResumeAnalysisRepository {

    suspend fun analyzeWithGemini(
        apiKey: String,
        resumeText: String,
        jobDescriptionText: String
    ): String {
        val prompt = buildPrompt(resumeText, jobDescriptionText)
        Client(apiKey = apiKey).use { client ->
            val response = client.models.generateContent("gemini-2.5-flash", prompt)
            return response.text ?: error("The model returned an empty response.")
        }
    }

    fun parseAnalysis(rawText: String): ResumeAnalysisResponse {
        val cleanJson = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val root = JSONObject(cleanJson)
        return ResumeAnalysisResponse(
            matchScore = root.optInt("matchScore").coerceIn(0, 100),
            formattingScore = root.optInt("formattingScore").coerceIn(0, 100),
            contentScore = root.optInt("contentScore").coerceIn(0, 100),
            missingKeywords = root.optJSONArray("missingKeywords").toMissingKeywords(),
            improvedSuggestions = root.optJSONArray("improvedSuggestions").toSuggestions()
        )
    }

    fun fallbackAnalysis(resumeText: String, jobDescriptionText: String): ResumeAnalysisResponse {
        val resumeWords = importantWords(resumeText)
        val jobWords = importantWords(jobDescriptionText)
        val missing = jobWords
            .filterNot { it in resumeWords }
            .take(6)
            .mapIndexed { index, keyword ->
                MissingKeyword(
                    keyword = keyword.replaceFirstChar { it.uppercase() },
                    impactPercent = (14 - index * 2).coerceAtLeast(4),
                    reason = "The role emphasizes this term, but it is not clearly represented in the resume."
                )
            }
        val overlap = jobWords.count { it in resumeWords }
        val match = if (jobWords.isEmpty()) 0 else ((overlap.toDouble() / jobWords.size) * 100).roundToInt()
        return ResumeAnalysisResponse(
            matchScore = match.coerceIn(34, 92),
            formattingScore = estimateFormattingScore(resumeText),
            contentScore = (match + 18).coerceIn(42, 94),
            missingKeywords = missing.ifEmpty {
                listOf(
                    MissingKeyword("Measurable impact", 8, "Quantified achievements make recruiter scanning faster."),
                    MissingKeyword("Role-specific tooling", 6, "Naming the tools from the description improves ATS alignment.")
                )
            },
            improvedSuggestions = listOf(
                ImprovedSuggestion(
                    originalText = resumeText.lineSequence().firstOrNull { it.length > 24 }?.take(140)
                        ?: "Responsible for delivering software features.",
                    suggestedRewrite = "Delivered production Android features using Kotlin, MVVM, and coroutine-based async workflows aligned to business goals.",
                    explanation = "The rewrite adds stack specificity, stronger ownership language, and clearer relevance to the target role."
                ),
                ImprovedSuggestion(
                    originalText = "Worked with teams to improve application quality.",
                    suggestedRewrite = "Partnered with product and QA to reduce defects through focused reviews, telemetry-driven fixes, and repeatable release checks.",
                    explanation = "The suggestion turns a broad responsibility into a concrete, outcome-oriented accomplishment."
                )
            )
        )
    }

    private fun buildPrompt(resumeText: String, jobDescriptionText: String): String = """
        You are an expert resume reviewer. Return only valid JSON with this exact schema:
        {
          "matchScore": 0-100,
          "formattingScore": 0-100,
          "contentScore": 0-100,
          "missingKeywords": [
            {"keyword": "string", "impactPercent": 0-100, "reason": "string"}
          ],
          "improvedSuggestions": [
            {"originalText": "string", "suggestedRewrite": "string", "explanation": "string"}
          ]
        }

        Resume:
        $resumeText

        Job description:
        $jobDescriptionText
    """.trimIndent()

    private fun JSONArray?.toMissingKeywords(): List<MissingKeyword> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.let {
                MissingKeyword(
                    keyword = it.optString("keyword"),
                    impactPercent = it.optInt("impactPercent").coerceIn(0, 100),
                    reason = it.optString("reason")
                )
            }
        }
    }

    private fun JSONArray?.toSuggestions(): List<ImprovedSuggestion> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.let {
                ImprovedSuggestion(
                    originalText = it.optString("originalText"),
                    suggestedRewrite = it.optString("suggestedRewrite"),
                    explanation = it.optString("explanation")
                )
            }
        }
    }

    private fun importantWords(text: String): Set<String> {
        val stopWords = setOf("and", "the", "with", "for", "you", "are", "that", "this", "from", "have", "will", "your")
        return Regex("[A-Za-z][A-Za-z+#.-]{2,}")
            .findAll(text.lowercase())
            .map { it.value.trim('.', ',', ';', ':') }
            .filter { it !in stopWords && it.length > 3 }
            .toSet()
    }

    private fun estimateFormattingScore(text: String): Int {
        var score = 58
        if (text.contains(Regex("\\b(experience|education|skills|projects)\\b", RegexOption.IGNORE_CASE))) score += 14
        if (text.lines().any { it.trim().startsWith("-") || it.trim().startsWith("*") }) score += 10
        if (text.length in 1200..6000) score += 10
        if (text.contains(Regex("\\d+%|\\$\\d+|\\b\\d+x\\b", RegexOption.IGNORE_CASE))) score += 8
        return score.coerceIn(40, 96)
    }
}
