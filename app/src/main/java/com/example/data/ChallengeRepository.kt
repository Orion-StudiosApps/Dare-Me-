package com.example.data

import com.example.BuildConfig
import com.example.api.*
import kotlinx.coroutines.flow.Flow
import java.net.URLDecoder
import java.net.URLEncoder

class ChallengeRepository(private val dao: ChallengeSessionDao) {

    val allSessions: Flow<List<ChallengeSession>> = dao.getAllSessionsFlow()

    suspend fun insertSession(session: ChallengeSession) {
        dao.insertSession(session)
    }

    suspend fun getTotalScore(): Int {
        return dao.getTotalScore() ?: 0
    }

    suspend fun getCompletedCount(): Int {
        return dao.getCompletedChallengesCount()
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    // Call Gemini to generate a chaotic challenge
    suspend fun generateChallengeFromAI(systemTheme: String = ""): ChallengeResponse {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API key is missing")
        }

        val prompt = if (systemTheme.isNotEmpty()) {
            "Generate a highly specific, funny, safe but absolutely chaotic social dare/challenge around the theme: '$systemTheme'. " +
                    "Make it something a user can describe completing in typing or with a mock photo."
        } else {
            "Generate a highly specific, funny, completely safe but absolutely chaotic, absurd social dare/challenge. " +
                    "Ensure it relies on everyday objects and is highly hilarious."
        }

        val systemInstruction = "You are a master of hilarious web-viral chaotic trends. Your job is to generate extremely specific, safe, highly funny, and slightly ridiculous 'dares' or 'challenges'. Give a punchy title and step-by-step playful instructions."

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(text = ResponseSchemaText(mimeType = "application/json")),
                temperature = 0.9
            )
        )

        val response = GeminiNetwork.service.generateContent(apiKey, request)
        val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Empty response from AI")

        // Parse utilizing Moshi
        val adapter = GeminiNetwork.getMoshi().adapter(ChallengeResponse::class.java)
        return adapter.fromJson(jsonText) ?: throw IllegalStateException("Failed to parse challenge")
    }

    // Call Gemini as the Referee to grade the user completion
    suspend fun gradeUserDareCompletion(
        challengeTitle: String,
        challengeDescription: String,
        userResponseText: String
    ): RefereeGradeResponse {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API key is missing")
        }

        val prompt = """
            Challenge Title: $challengeTitle
            Challenge Instructions: $challengeDescription
            User's completion proof/description: "$userResponseText"
            
            Evaluate this response. Grade it on a scale of 1-100 based on creativity, humor, chaotic willingness, and effort. 
            Write a hilarious, slightly roasting, highly sarcastic but witty evaluation comment. Be comically critical but friendly (like an elegant roaster).
            Also name a funny unique 'achievementUnlocked' title (e.g. "Toasted Bread Enabler", "Sloth Overlord").
            Classify their matching rankTier based on their score:
            - Under 40: "PAThetic MUNDANE"
            - 40-69: "AMATEUR CHAOS INITIATE"
            - 70-89: "SOUP LORD"
            - 90-100: "SUPREME CHAOS MONARCH"
        """.trimIndent()

        val systemInstruction = "You are the Sarcastic Dare Referee. You are hilarious, witty, highly critique-heavy, and slightly condescending but comically supportive of creative, chaotic acts. You always return a valid JSON object matching the requested schema."

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(text = ResponseSchemaText(mimeType = "application/json")),
                temperature = 0.8
            )
        )

        val response = GeminiNetwork.service.generateContent(apiKey, request)
        val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Empty review from Referee")

        val adapter = GeminiNetwork.getMoshi().adapter(RefereeGradeResponse::class.java)
        return adapter.fromJson(jsonText) ?: throw IllegalStateException("Failed to parse referee grade")
    }
}
