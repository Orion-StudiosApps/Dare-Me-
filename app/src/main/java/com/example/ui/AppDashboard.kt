package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.api.ChallengeResponse
import com.example.api.RefereeGradeResponse
import com.example.ui.theme.*
import com.example.viewmodel.ChallengeState
import com.example.viewmodel.ChallengeViewModel
import com.example.viewmodel.GradeState
import com.example.viewmodel.LeaderboardEntry

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppDashboard(
    viewModel: ChallengeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val challengeState by viewModel.challengeState.collectAsStateWithLifecycle()
    val gradeState by viewModel.gradeState.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val streakCount by viewModel.streakCount.collectAsStateWithLifecycle()
    val totalScore by viewModel.totalScore.collectAsStateWithLifecycle()
    val isApiKeyAlertVisible by viewModel.isApiKeyAlertVisible.collectAsStateWithLifecycle()

    var submissionText by remember { mutableStateOf("") }
    var customThemeText by remember { mutableStateOf("") }
    var isLeaderboardExpanded by remember { mutableStateOf(false) }

    // Floating Referee Grade dialogue popup
    if (gradeState is GradeState.Success) {
        RefereeGradeDialog(
            gradeResult = (gradeState as GradeState.Success).grade,
            challengeTitle = (gradeState as GradeState.Success).title,
            onDismiss = {
                viewModel.dismissGradeState()
                submissionText = "" // clear user response fields
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
    ) {
        // Core dashboard content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. Hero Brand Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Banner Image loader with custom overlay gradient
                Image(
                    painter = painterResource(id = R.drawable.img_banner),
                    contentDescription = "DareMe Splash Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    CyberBg.copy(alpha = 0.5f),
                                    CyberBg
                                )
                            )
                        )
                )

                // App title text overlay inside banner
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DAREME",
                            color = CyberPrimary,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("app_title")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(CyberTertiary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE AI",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Outlast. Out-absurd. Share the Chaos.",
                        color = CyberWhite.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // API Level warning indicator (Only shows if Gemini key is placeholder or empty)
            if (isApiKeyAlertVisible) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSecondary.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, CyberSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "API Alert",
                            tint = CyberTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Running in Demo Fallback Mode",
                                color = CyberTertiary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "To unlock infinite chaotic AI-generated challenges, enter your GEMINI_API_KEY into the Secrets panel.",
                                color = CyberWhite.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // 2. Metrics Scorebar Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Streak Counter Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("streak_count_badge"),
                    colors = CardDefaults.cardColors(containerColor = CyberCard)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🔥 CURRENT STREAK", color = CyberPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (streakCount > 0) "$streakCount Days" else "0 Days",
                            color = CyberWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Total Score Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CyberCard)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🏆 TOTAL SCORE", color = CyberTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalScore Pts",
                            color = CyberWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // 3. active Challenge Card Arena
            ChallengeCardArena(
                state = challengeState,
                gradeState = gradeState,
                submissionText = submissionText,
                customThemeText = customThemeText,
                onSubmissionTextChange = { submissionText = it },
                onThemeTextChange = { customThemeText = it },
                onReroll = { theme ->
                    viewModel.getRerollChallenge(theme)
                    customThemeText = ""
                },
                onSubmit = { title, desc ->
                    viewModel.submitChallengeResponse(title, desc, submissionText)
                }
            )

            // 4. Live Leaderboard Section (Climb Ranks!)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                border = BorderStroke(1.dp, CyberSecondary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isLeaderboardExpanded = !isLeaderboardExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Leaderboard Logo",
                                tint = CyberTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Chaotic Global Standings",
                                color = CyberWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isLeaderboardExpanded) "Collapse" else "View All",
                                color = CyberTertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (isLeaderboardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Leaderboard",
                                tint = CyberTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Real-time mock challenges from rival internet agents.",
                        color = CyberWhite.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Leaderboard Listing column
                    val visibleLeaderboard = if (isLeaderboardExpanded) leaderboard else leaderboard.take(3)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("leaderboard_list")
                    ) {
                        visibleLeaderboard.forEachIndexed { index, entry ->
                            LeaderboardRowItem(index = index, entry = entry)
                        }
                    }
                }
            }

            // 5. Historical Completed Dares
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Achievements Log",
                        color = CyberWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (history.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearHistory() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Clear History",
                                tint = CyberPrimary.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .border(1.dp, CyberSecondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "No completed challenges",
                                tint = CyberWhite.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No completed dares yet. Unleash chaos above!",
                                color = CyberWhite.copy(alpha = 0.4f),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        history.forEach { session ->
                            CompletedChallengeRowItem(session)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// --- Leaderboard Sub-component ---
@Composable
fun LeaderboardRowItem(index: Int, entry: LeaderboardEntry) {
    val rankColorAndLabel = when (index) {
        0 -> Pair(CyberYellow, "🥇")
        1 -> Pair(Color(0xFFC0C0C0), "🥈")
        2 -> Pair(Color(0xFFCD7F32), "🥉")
        else -> Pair(CyberWhite.copy(alpha = 0.5f), "${index + 1}")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (entry.isUser) CyberPrimary.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                1.dp,
                if (entry.isUser) CyberPrimary else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placement / Rank indicator
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rankColorAndLabel.second,
                color = rankColorAndLabel.first,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Avatar text and info
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = entry.username,
            color = if (entry.isUser) CyberPrimary else CyberWhite,
            fontWeight = if (entry.isUser) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Tier / status
        Box(
            modifier = Modifier
                .background(CyberSecondary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = entry.rankTitle.uppercase(),
                color = CyberTertiary,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Total score points
        Text(
            text = "${entry.score} pts",
            color = CyberWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

// --- Completed Dare historical item ---
@Composable
fun CompletedChallengeRowItem(session: com.example.data.ChallengeSession) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = CyberCard.copy(alpha = 0.7f)),
        border = BorderStroke(0.5.dp, CyberTertiary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.challengeTitle,
                        color = CyberWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Score: ${session.score} (${session.rankTier})",
                        color = if (session.score >= 90) CyberGreen else CyberTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand past session",
                    tint = CyberWhite.copy(alpha = 0.5f)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider(color = CyberTertiary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Your submission:",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = session.userResponse,
                        color = CyberWhite.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = "AI Referee evaluation:",
                        color = CyberTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = session.gradeComment,
                        color = CyberWhite,
                        fontSize = 12.sp,
                        style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    )
                }
            }
        }
    }
}


// --- Active Challenge Card & Text Input Field ---
@Composable
fun ChallengeCardArena(
    state: ChallengeState,
    gradeState: GradeState,
    submissionText: String,
    customThemeText: String,
    onSubmissionTextChange: (String) -> Unit,
    onThemeTextChange: (String) -> Unit,
    onReroll: (String) -> Unit,
    onSubmit: (String, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        border = BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚡ TODAY'S CHALLENGE",
                color = CyberPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(10.dp))

            when (state) {
                is ChallengeState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = CyberPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "AI Referee is searching the chaos databases...",
                                color = CyberWhite.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                is ChallengeState.Success -> {
                    ChallengeInfoBlock(state.challenge)
                    ChallengeInputSubmissionArea(
                        title = state.challenge.challengeTitle,
                        desc = state.challenge.challengeDescription,
                        gradeState = gradeState,
                        submissionText = submissionText,
                        onSubmissionTextChange = onSubmissionTextChange,
                        customThemeText = customThemeText,
                        onThemeTextChange = onThemeTextChange,
                        onReroll = onReroll,
                        onSubmit = onSubmit
                    )
                }

                is ChallengeState.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberSecondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = "Network Error", tint = CyberYellow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Failed to load dynamic AI dare. Swapped with offline fallback!",
                            color = CyberYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ChallengeInfoBlock(state.fallback)
                    ChallengeInputSubmissionArea(
                        title = state.fallback.challengeTitle,
                        desc = state.fallback.challengeDescription,
                        gradeState = gradeState,
                        submissionText = submissionText,
                        onSubmissionTextChange = onSubmissionTextChange,
                        customThemeText = customThemeText,
                        onThemeTextChange = onThemeTextChange,
                        onReroll = onReroll,
                        onSubmit = onSubmit
                    )
                }

                else -> {
                    Text(
                        text = "Loading challenge details...",
                        color = CyberWhite,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChallengeInfoBlock(challenge: ChallengeResponse) {
    Column {
        Text(
            text = challenge.challengeTitle,
            color = CyberWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.testTag("challenge_title")
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = challenge.challengeDescription,
            color = CyberWhite.copy(alpha = 0.85f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChallengeInputSubmissionArea(
    title: String,
    desc: String,
    gradeState: GradeState,
    submissionText: String,
    onSubmissionTextChange: (String) -> Unit,
    customThemeText: String,
    onThemeTextChange: (String) -> Unit,
    onReroll: (String) -> Unit,
    onSubmit: (String, String) -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))

    // Interactive Theme field to trigger targeted challenges
    OutlinedTextField(
        value = customThemeText,
        onValueChange = onThemeTextChange,
        label = { Text("Generate specific dare theme (e.g. food, pet, gym)", color = CyberWhite.copy(alpha = 0.6f)) },
        placeholder = { Text("Leave blank for complete random chaos") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberSecondary,
            unfocusedBorderColor = CyberWhite.copy(alpha = 0.2f),
            focusedTextColor = CyberWhite,
            unfocusedTextColor = CyberWhite
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        maxLines = 1,
        trailingIcon = {
            IconButton(
                onClick = { onReroll(customThemeText) },
                modifier = Modifier.testTag("reroll_theme_button")
            ) {
                Icon(Icons.Default.Star, contentDescription = "Reroll Theme", tint = CyberPrimary)
            }
        }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // user response input box
    OutlinedTextField(
        value = submissionText,
        onValueChange = onSubmissionTextChange,
        label = { Text("Describe your hilarious attempt here...", color = CyberPrimary) },
        placeholder = { Text("e.g. I stared at the bowl of spinach for 40 seconds. I reasoned that high potassium represents a form of higher consciousness. Spinach agreed.") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberPrimary,
            unfocusedBorderColor = CyberSecondary.copy(alpha = 0.4f),
            focusedTextColor = CyberWhite,
            unfocusedTextColor = CyberWhite
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .testTag("user_response_input"),
        shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Reroll button configuration
        OutlinedButton(
            onClick = { onReroll("") },
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("reroll_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTertiary),
            border = BorderStroke(1.dp, CyberTertiary)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reroll")
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Reroll", fontWeight = FontWeight.Bold)
        }

        // Submit completion button
        Button(
            onClick = { onSubmit(title, desc) },
            enabled = gradeState !is GradeState.Submitting,
            modifier = Modifier
                .weight(1.5f)
                .height(48.dp)
                .testTag("submit_proof_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberPrimary,
                disabledContainerColor = CyberPrimary.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (gradeState is GradeState.Submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = CyberWhite,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Done, contentDescription = "Check")
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Submit to Referee", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}


// --- Spotify-Wrapped Style Floating Grade Review Card Overlay ---
@Composable
fun RefereeGradeDialog(
    gradeResult: RefereeGradeResponse,
    challengeTitle: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Share result using native platform intent
    fun shareMemeCardIntent() {
        val shareMessage = """
            🔥 I BEAT THE DARE ON DAREME AI!
            ⚡ Dare: "$challengeTitle"
            
            👑 SCORE: ${gradeResult.score}/100 
            ⭐️ RANK: ${gradeResult.rankTier.uppercase()}
            🏆 UNLOCKED: "${gradeResult.achievementUnlocked}"
            
            💬 AI REFEREE COMMENTS: 
            "${gradeResult.gradeComment}"
            
            Can you out-chaos me? Complete daily AI dares now! 🚀
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "DareMe: AI Challenge Scorecard")
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }
        context.startActivity(Intent.createChooser(intent, "Share scorecard using"))
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(CyberBg)
                .border(2.dp, CyberPrimary, RoundedCornerShape(24.dp))
                .padding(2.dp)
                .testTag("share_card_container")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                CyberSecondary.copy(alpha = 0.4f),
                                CyberBg
                            )
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top header
                Text(
                    text = "OFFICIAL SCORECARD",
                    color = CyberTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Score dial badge
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(CyberCard)
                        .border(
                            4.dp,
                            Brush.sweepGradient(
                                listOf(CyberPrimary, CyberSecondary, CyberTertiary, CyberPrimary)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${gradeResult.score}",
                            color = when {
                                gradeResult.score >= 90 -> CyberGreen
                                gradeResult.score >= 70 -> CyberTertiary
                                else -> CyberPrimary
                            },
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "PTS",
                            color = CyberWhite.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Rank badge label
                Text(
                    text = gradeResult.rankTier.uppercase(),
                    color = CyberPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Achievement badge label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(CyberSecondary.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Achievement Icon",
                        tint = CyberYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "UNLOCKED: ${gradeResult.achievementUnlocked}",
                        color = CyberYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sarcastic Comment Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberCard)
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "💬 REFEREE VERDICT",
                            color = CyberTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${gradeResult.gradeComment}\"",
                            color = CyberWhite,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Control buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Close button config
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("close_grade_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCard),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Dismiss", color = CyberWhite)
                    }

                    // Share cards and bragging rights trigger
                    Button(
                        onClick = { shareMemeCardIntent() },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("share_intent_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share on Socials")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Brag to Socials", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
