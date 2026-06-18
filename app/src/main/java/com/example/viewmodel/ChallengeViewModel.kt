package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.ChallengeResponse
import com.example.api.RefereeGradeResponse
import com.example.data.AppDatabase
import com.example.data.ChallengeRepository
import com.example.data.ChallengeSession
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ChallengeState {
    object Idle : ChallengeState
    object Loading : ChallengeState
    data class Success(val challenge: ChallengeResponse, val isFromAI: Boolean) : ChallengeState
    data class Error(val message: String, val fallback: ChallengeResponse) : ChallengeState
}

sealed interface GradeState {
    object Idle : GradeState
    object Submitting : GradeState
    data class Success(val grade: RefereeGradeResponse, val title: String) : GradeState
    data class Error(val message: String) : GradeState
}

data class LeaderboardEntry(
    val username: String,
    val score: Int,
    val avatarEmoji: String,
    val rankTitle: String,
    val isUser: Boolean = false
)

class ChallengeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ChallengeRepository(db.challengeSessionDao())

    private val prefs = application.getSharedPreferences("dareme_prefs", Context.MODE_PRIVATE)

    // Fallback static challenges
    private val fallbackChallenges = listOf(
        ChallengeResponse(
            "The High-Density Stare",
            "Place any glass of tap water in front of you. Stare at it with complete, unblinking dramatic intensity for 30 seconds. Write a 2-sentence poetic description about its historical journey and feelings."
        ),
        ChallengeResponse(
            "The Spoon Negotiator",
            "Hold a metallic spoon up to your face and convince it that it should be knighted for its exceptional service in pudding. Describe its heroic qualities."
        ),
        ChallengeResponse(
            "Geopolitical Sock Matchup",
            "Put a sock on both hands. Have them engage in a silent, slow-motion debate about the philosophical superiority of cats versus dogs. Write down which sock won and its finishing argument."
        ),
        ChallengeResponse(
            "The Soft Roar",
            "Go into the nearest corner of your room and whisper a highly dramatic, slow-motion movie trailer roar for 10 seconds. Describe the invisible monster you just summoned."
        ),
        ChallengeResponse(
            "The Invisible Chef",
            "Pretend you are presenting a Michelin Star gourmet dish to a critic, but the plate is completely empty. Describe the complex culinary profile and texture of the oxygen."
        )
    )

    private val _challengeState = MutableStateFlow<ChallengeState>(ChallengeState.Idle)
    val challengeState: StateFlow<ChallengeState> = _challengeState.asStateFlow()

    private val _gradeState = MutableStateFlow<GradeState>(GradeState.Idle)
    val gradeState: StateFlow<GradeState> = _gradeState.asStateFlow()

    // History from Room DB
    val history: StateFlow<List<ChallengeSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Metrics
    private val _streakCount = MutableStateFlow(prefs.getInt("streak_count", 0))
    val streakCount: StateFlow<Int> = _streakCount.asStateFlow()

    private val _totalScore = MutableStateFlow(0)
    val totalScore: StateFlow<Int> = _totalScore.asStateFlow()

    private val _isApiKeyAlertVisible = MutableStateFlow(false)
    val isApiKeyAlertVisible: StateFlow<Boolean> = _isApiKeyAlertVisible.asStateFlow()

    // Global Simulated Leaderboard with fun characters
    private val baseCompetitors = listOf(
        LeaderboardEntry("SassyPanda 🐼", 2850, "🐼", "Soup Lord"),
        LeaderboardEntry("GigaCabbage 🥬", 2400, "🥬", "Cabbage Master"),
        LeaderboardEntry("ChaosConnoisseur 💥", 1950, "💥", "Chaos Monarch"),
        LeaderboardEntry("DramaticToast 🍞", 1550, "🍞", "Bread Messiah"),
        LeaderboardEntry("MemeOverlord 👾", 1100, "👾", "Soup Lord"),
        LeaderboardEntry("QuietSloth 🦥", 620, "🦥", "Amateur Initiate")
    )

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    init {
        loadMetrics()
        getRerollChallenge()
        checkApiKeyAvailability()
    }

    fun checkApiKeyAvailability() {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        _isApiKeyAlertVisible.value = apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY"
    }

    private fun loadMetrics() {
        viewModelScope.launch {
            val score = repository.getTotalScore()
            _totalScore.value = score
            updateLeaderboard(score)
        }
    }

    private fun updateLeaderboard(userTotalScore: Int) {
        val userRank = when {
            userTotalScore < 100 -> "Pathetic Mundane"
            userTotalScore < 500 -> "Amateur Initiate"
            userTotalScore < 1500 -> "Soup Lord"
            else -> "Supreme Chaos Monarch"
        }
        val userEntry = LeaderboardEntry("You (Legend) ⭐", userTotalScore, "👑", userRank, isUser = true)
        val allEntries = (baseCompetitors + userEntry).sortedByDescending { it.score }
        _leaderboard.value = allEntries
    }

    fun getRerollChallenge(theme: String = "") {
        _challengeState.value = ChallengeState.Loading
        _gradeState.value = GradeState.Idle
        viewModelScope.launch {
            try {
                val challenge = repository.generateChallengeFromAI(theme)
                _challengeState.value = ChallengeState.Success(challenge, isFromAI = true)
            } catch (e: Exception) {
                // Fallback graceful degradation
                val randomFallback = fallbackChallenges.random()
                _challengeState.value = ChallengeState.Error(
                    e.message ?: "Network error",
                    randomFallback
                )
            }
        }
    }

    fun submitChallengeResponse(challengeTitle: String, challengeDescription: String, responseText: String) {
        if (responseText.trim().isEmpty()) {
            _gradeState.value = GradeState.Error("You cannot submit an empty challenge proof! Show some effort!")
            return
        }

        _gradeState.value = GradeState.Submitting
        viewModelScope.launch {
            try {
                // Grade Dare with AI
                val gradeResult = repository.gradeUserDareCompletion(
                    challengeTitle,
                    challengeDescription,
                    responseText
                )

                // Save to Room DB
                val session = ChallengeSession(
                    challengeTitle = challengeTitle,
                    challengeDescription = challengeDescription,
                    userResponse = responseText,
                    score = gradeResult.score,
                    gradeComment = gradeResult.gradeComment,
                    rankTier = gradeResult.rankTier
                )
                repository.insertSession(session)

                // Increment streak
                val newStreak = _streakCount.value + 1
                _streakCount.value = newStreak
                prefs.edit().putInt("streak_count", newStreak).apply()

                // Reload metrics & leaderboard
                loadMetrics()

                _gradeState.value = GradeState.Success(gradeResult, challengeTitle)

            } catch (e: Exception) {
                // Local fallback referee in case of API failure / mismatch
                val fallbackScore = (60..99).random()
                val funnyComment = "My sensors are experiencing temporary cosmic dust, but your effort looks remarkably decent! You demonstrated genuine comedic courage under duress."
                val funnyAchievement = "Dust Buster"
                val fallbackRank = when {
                    fallbackScore < 70 -> "Amateur Initiate"
                    fallbackScore < 90 -> "Soup Lord"
                    else -> "Supreme Chaos Monarch"
                }

                val gradeResult = RefereeGradeResponse(
                    score = fallbackScore,
                    gradeComment = funnyComment,
                    achievementUnlocked = funnyAchievement,
                    rankTier = fallbackRank
                )

                // Save to DB
                val session = ChallengeSession(
                    challengeTitle = challengeTitle,
                    challengeDescription = challengeDescription,
                    userResponse = responseText,
                    score = fallbackScore,
                    gradeComment = funnyComment,
                    rankTier = fallbackRank
                    )
                repository.insertSession(session)

                // Update metrics
                val newStreak = _streakCount.value + 1
                _streakCount.value = newStreak
                prefs.edit().putInt("streak_count", newStreak).apply()

                loadMetrics()

                _gradeState.value = GradeState.Success(gradeResult, challengeTitle)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _streakCount.value = 0
            prefs.edit().putInt("streak_count", 0).apply()
            loadMetrics()
        }
    }

    fun dismissGradeState() {
        _gradeState.value = GradeState.Idle
    }
}
