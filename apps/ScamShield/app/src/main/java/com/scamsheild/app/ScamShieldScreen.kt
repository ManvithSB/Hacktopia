package com.scamsheild.app

// ─────────────────────────────────────────────────────────────────────────────
// ScamShield Dashboard — v4 (Backend Integration + Auto SMS Protection)
//
// HTTP transport: java.net.HttpURLConnection (no new Gradle dependencies).
// Endpoint:       POST http://10.0.2.2:8000/analyze
//                 10.0.2.2 = Android emulator alias for host loopback.
// Request body:   {"message":"...","language":"en"}
// Response:       {"risk_level","score","reasons","recommendation","signals",...}
//
// v4 adds: Automatic SMS Protection section (toggle + settings launcher).
// PROTOTYPE: local hackathon demo only. Not Play Store policy compliant.
// ─────────────────────────────────────────────────────────────────────────────

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.scamsheild.app.ui.theme.BorderColor
import com.scamsheild.app.ui.theme.NavyCard
import com.scamsheild.app.ui.theme.NavyDeep
import com.scamsheild.app.ui.theme.NavyMid
import com.scamsheild.app.ui.theme.NavySurface
import com.scamsheild.app.ui.theme.RiskHigh
import com.scamsheild.app.ui.theme.RiskHighBg
import com.scamsheild.app.ui.theme.RiskSafe
import com.scamsheild.app.ui.theme.RiskSafeBg
import com.scamsheild.app.ui.theme.RiskSuspicious
import com.scamsheild.app.ui.theme.RiskSuspiciousBg
import com.scamsheild.app.ui.theme.TealBright
import com.scamsheild.app.ui.theme.TealGlow
import com.scamsheild.app.ui.theme.TextMuted
import com.scamsheild.app.ui.theme.TextPrimary
import com.scamsheild.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// BACKEND CONFIGURATION
// ═════════════════════════════════════════════════════════════════════════════

/**
 * 10.0.2.2 is the Android emulator's special alias for the host machine's
 * loopback address (127.0.0.1).  The FastAPI server listens on port 8000.
 */
private const val BACKEND_URL = "http://10.149.198.250:8000/analyze"
private const val CONNECT_TIMEOUT_MS = 5_000
private const val READ_TIMEOUT_MS    = 15_000

// ═════════════════════════════════════════════════════════════════════════════
// DATA MODELS
// ═════════════════════════════════════════════════════════════════════════════

/** Mirrors the backend's risk_level values exactly. */
enum class RiskLevel { SAFE, SUSPICIOUS, HIGH_RISK }

/** Mirrors the AnalyzeResponse Pydantic schema. */
data class AnalysisResult(
    val riskLevel: RiskLevel,
    val score: Int,
    val reasons: List<String>,
    val recommendedAction: String,   // mapped from recommendation (PROCEED/VERIFY/STOP)
    val signals: List<String>,
    val interactionId: String
)

data class ScanRecord(
    val preview: String,
    val result: AnalysisResult,
    val timestamp: String
)

// ═════════════════════════════════════════════════════════════════════════════
// SAMPLE MESSAGES
// ═════════════════════════════════════════════════════════════════════════════

private const val SAMPLE_SAFE =
    "Hey! Just wanted to check if we're still on for lunch tomorrow at 12:30 pm. " +
    "Let me know if anything changes. Looking forward to seeing you!"

private const val SAMPLE_SUSPICIOUS =
    "Congratulations! You have been selected for a special reward. " +
    "Click the link to claim your prize: http://rewards-claim-now.net/get?id=8823"

private const val SAMPLE_HIGH_RISK =
    "URGENT: Your bank account has been locked due to suspicious activity. " +
    "Verify your identity NOW or lose access permanently. " +
    "Call +1-800-555-FAKE or click: http://secure-bank-verify.xyz/login. " +
    "Share your OTP to re-activate."

// ═════════════════════════════════════════════════════════════════════════════
// HTTP CLIENT — calls POST /analyze on the FastAPI backend
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Sends the message to the FastAPI backend and parses the JSON response.
 * Runs on [Dispatchers.IO] — never call from the main thread.
 *
 * Throws [BackendException] for any network / HTTP / parse failure so the
 * UI can show a specific error message rather than silently falling back.
 */
class BackendException(message: String, val httpCode: Int = -1) : Exception(message)

private suspend fun callBackend(message: String): AnalysisResult = withContext(Dispatchers.IO) {
    val conn = try {
        (URL(BACKEND_URL).openConnection() as HttpURLConnection).apply {
            requestMethod  = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout    = READ_TIMEOUT_MS
            doOutput       = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept",       "application/json")
        }
    } catch (e: Exception) {
        throw BackendException("Cannot reach backend at $BACKEND_URL: ${e.message}")
    }

    try {
        // Build request JSON — only the fields the Android app supplies
        val body = JSONObject().apply {
            put("message",  message)
            put("language", "en")
        }.toString()

        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body) }

        val code = conn.responseCode
        if (code != 200) {
            // Try to read the error body for a useful message
            val errBody = runCatching {
                BufferedReader(InputStreamReader(conn.errorStream)).readText()
            }.getOrElse { "no detail" }
            throw BackendException("Backend returned HTTP $code: $errBody", code)
        }

        val responseText = BufferedReader(InputStreamReader(conn.inputStream)).readText()
        parseResponse(responseText)

    } catch (e: BackendException) {
        throw e
    } catch (e: Exception) {
        throw BackendException("Network error: ${e.message}")
    } finally {
        conn.disconnect()
    }
}

/**
 * Maps the raw JSON response to [AnalysisResult].
 * Handles the exact field names from AnalyzeResponse in schemas.py.
 */
private fun parseResponse(json: String): AnalysisResult {
    return try {
        val obj = JSONObject(json)

        // risk_level: "SAFE" | "SUSPICIOUS" | "HIGH_RISK"
        val riskLevel = when (obj.getString("risk_level").uppercase()) {
            "HIGH_RISK"  -> RiskLevel.HIGH_RISK
            "SUSPICIOUS" -> RiskLevel.SUSPICIOUS
            else         -> RiskLevel.SAFE
        }

        // recommendation: "PROCEED" | "VERIFY" | "STOP"
        // Store the raw code; the Composable will localize it via string resources.
        val action = when (obj.optString("recommendation", "PROCEED").uppercase()) {
            "STOP"   -> "__STOP__"
            "VERIFY" -> "__VERIFY__"
            else     -> "__PROCEED__"
        }

        // reasons: List<String>
        val reasonsArr = obj.optJSONArray("reasons")
        val reasons = if (reasonsArr != null) {
            (0 until reasonsArr.length()).map { reasonsArr.getString(it) }
        } else {
            listOf("No reasons returned by server.")
        }

        // signals: List<String>
        val signalsArr = obj.optJSONArray("signals")
        val signals = if (signalsArr != null) {
            (0 until signalsArr.length()).map { signalsArr.getString(it) }
        } else {
            emptyList()
        }

        AnalysisResult(
            riskLevel         = riskLevel,
            score             = obj.optInt("score", 0),
            reasons           = reasons.ifEmpty { listOf("No specific reasons returned.") },
            recommendedAction = action,
            signals           = signals,
            interactionId     = obj.optString("interaction_id", "")
        )
    } catch (e: Exception) {
        throw BackendException("Failed to parse server response: ${e.message}")
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// RISK METADATA HELPER
// ═════════════════════════════════════════════════════════════════════════════

private data class RiskMeta(
    val color: Color,
    val bgColor: Color,
    val label: String,
    val emoji: String,
    val chipLabel: String
)

private fun riskMetaFor(level: RiskLevel): RiskMeta = when (level) {
    RiskLevel.SAFE       -> RiskMeta(RiskSafe,      RiskSafeBg,       "__SAFE__",      "\u2705",          "__SAFE__")
    RiskLevel.SUSPICIOUS -> RiskMeta(RiskSuspicious, RiskSuspiciousBg, "__SUSPICIOUS__", "\u26A0\uFE0F",    "__SUSPICIOUS__")
    RiskLevel.HIGH_RISK  -> RiskMeta(RiskHigh,       RiskHighBg,       "__HIGH_RISK__",  "\uD83D\uDED1",    "__HIGH_RISK__")
}

/** Resolves a recommendation code like __STOP__ to a localized string. */
@Composable
private fun localizedRecommendation(code: String): String = when (code) {
    "__STOP__"    -> stringResource(R.string.rec_stop)
    "__VERIFY__"  -> stringResource(R.string.rec_verify)
    else          -> stringResource(R.string.rec_proceed)
}

/** Resolves a risk-level code like __HIGH_RISK__ to a localized display label. */
@Composable
private fun localizedRiskLabel(code: String): String = when (code) {
    "__HIGH_RISK__"  -> stringResource(R.string.risk_high_risk)
    "__SUSPICIOUS__" -> stringResource(R.string.risk_suspicious)
    else             -> stringResource(R.string.risk_safe)
}

/** Maps known backend signal/reason identifiers to their localized reason string. */
@Composable
private fun localizedReason(raw: String): String {
    // The backend SIGNAL_REASONS strings are stable English sentences.
    // We match them to their signal code, then serve the localized string.
    return when {
        raw.contains("urgency") || raw.contains("act quickly") || raw.contains("urgent") ->
            stringResource(R.string.reason_urgent_language)
        raw.contains("account blocking") || raw.contains("suspension") || raw.contains("account_threat") ->
            stringResource(R.string.reason_account_threat)
        raw.contains("KYC") || raw.contains("kyc") ->
            stringResource(R.string.reason_kyc_pressure)
        raw.contains("impersonat") || raw.contains("authority") || raw.contains("bank or official") ->
            stringResource(R.string.reason_impersonation)
        raw.contains("OTP") || raw.contains("PIN") || raw.contains("credential") || raw.contains("card details") ->
            stringResource(R.string.reason_credential_request)
        raw.contains("payment") || raw.contains("transfer money") || raw.contains("pay") ->
            stringResource(R.string.reason_payment_pressure)
        raw.contains("verification") || raw.contains("unlocking") || raw.contains("refund or reward") ->
            stringResource(R.string.reason_suspicious_intent)
        raw.contains("prize") || raw.contains("lottery") ->
            stringResource(R.string.reason_lottery_prize)
        raw.contains("cashback") || raw.contains("refund") || raw.contains("reward claim") ->
            stringResource(R.string.reason_refund_reward)
        else -> raw  // Groq free-form or unknown: show as-is
    }
}

/** Maps known signal codes to localized chip labels. */
@Composable
private fun localizedSignal(raw: String): String = when (raw.lowercase()) {
    "urgent_language"     -> stringResource(R.string.signal_urgent_language)
    "account_threat"      -> stringResource(R.string.signal_account_threat)
    "kyc_pressure"        -> stringResource(R.string.signal_kyc_pressure)
    "impersonation"       -> stringResource(R.string.signal_impersonation)
    "credential_request"  -> stringResource(R.string.signal_credential_request)
    "payment_pressure"    -> stringResource(R.string.signal_payment_pressure)
    "suspicious_intent"   -> stringResource(R.string.signal_suspicious_intent)
    "lottery_prize_scam"  -> stringResource(R.string.signal_lottery_prize)
    "refund_reward_scam"  -> stringResource(R.string.signal_refund_reward)
    else                  -> raw.replace("_", " ")
}

// ═════════════════════════════════════════════════════════════════════════════
// UI STATE
// ═════════════════════════════════════════════════════════════════════════════

private sealed class ScreenState {
    object Idle    : ScreenState()
    object Loading : ScreenState()
    data class Error(val message: String) : ScreenState()
    data class Success(val result: AnalysisResult) : ScreenState()
}

// ═════════════════════════════════════════════════════════════════════════════
// HELPERS
// ═════════════════════════════════════════════════════════════════════════════

private val TIME_FMT = SimpleDateFormat("h:mm a", Locale.getDefault())
private fun now(): String = TIME_FMT.format(Date())

// ═════════════════════════════════════════════════════════════════════════════
// ROOT SCREEN COMPOSABLE
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun ScamShieldScreen() {
    var messageText by remember { mutableStateOf("") }
    var screenState by remember { mutableStateOf<ScreenState>(ScreenState.Idle) }
    val recentScans = remember { mutableStateListOf<ScanRecord>() }
    val scope       = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun analyze(text: String) {
        val trimmed = text.trim()
        if (trimmed.length < 10) {
            screenState = ScreenState.Error("Message is too short. Please paste at least one full sentence.")
            return
        }
        screenState = ScreenState.Loading
        scope.launch {
            try {
                val result = callBackend(trimmed)
                screenState = ScreenState.Success(result)
                recentScans.add(
                    0,
                    ScanRecord(
                        preview   = trimmed.take(60) + if (trimmed.length > 60) "..." else "",
                        result    = result,
                        timestamp = now()
                    )
                )
                if (recentScans.size > 5) recentScans.removeAt(recentScans.lastIndex)
            } catch (e: BackendException) {
                screenState = ScreenState.Error(e.message ?: "Unknown backend error.")
            } catch (e: Exception) {
                screenState = ScreenState.Error("Unexpected error: ${e.message}")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, NavyMid, NavyMid)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LanguageSelector()
            DashboardHeader()
            ProtectionStatusCard(scanCount = recentScans.size)
            SectionLabel(text = stringResource(R.string.section_analyze))
            MessageInputCard(
                messageText  = messageText,
                isAnalyzing  = screenState is ScreenState.Loading,
                onTextChange = {
                    messageText = it
                    if (screenState is ScreenState.Error) screenState = ScreenState.Idle
                },
                onAnalyze    = { analyze(messageText) }
            )
            SampleMessageRow { sample ->
                messageText = sample
                screenState = ScreenState.Idle
            }
            ResultSection(state = screenState)
            AnimatedVisibility(
                visible = recentScans.isNotEmpty(),
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel(text = "RECENT SCANS")
                    recentScans.forEach { scan -> RecentScanRow(record = scan) }
                }
            }
            // ── Auto SMS Protection ───────────────────────────────────────
            SectionLabel(text = "AUTOMATIC PROTECTION")
            AutoSmsProtectionSection()
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// HEADER
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun DashboardHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "shieldPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.10f,
        animationSpec = infiniteRepeatable(tween(1_300), RepeatMode.Reverse),
        label         = "pulseScale"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(TealGlow)
        ) {
            Text(text = "\uD83D\uDEE1\uFE0F", fontSize = 32.sp, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text  = "ScamShield",
                color = TextPrimary,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                text          = "AI-Powered Scam Detection",
                color         = TealBright,
                style         = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.2.sp
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// PROTECTION STATUS CARD
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProtectionStatusCard(scanCount: Int) {
    val pulse = rememberInfiniteTransition(label = "dotPulse")
    val dotAlpha by pulse.animateFloat(
        initialValue  = 1f,
        targetValue   = 0.35f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "dotAlpha"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = NavySurface),
        border   = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(RiskSafe.copy(alpha = dotAlpha))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "Protection Active",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text  = "Connected to ScamShield backend \u2022 port 8000",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text  = "$scanCount",
                    color = TealBright,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text  = if (scanCount == 1) "scan" else "scans",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// MESSAGE INPUT CARD
// ═════════════════════════════════════════════════════════════════════════════

private const val MAX_CHARS = 1000

@Composable
private fun MessageInputCard(
    messageText  : String,
    isAnalyzing  : Boolean,
    onTextChange : (String) -> Unit,
    onAnalyze    : () -> Unit
) {
    val charCount = messageText.length
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = NavySurface),
        border   = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text  = "Paste Suspicious Message",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = "Copy and paste any SMS, email, or chat message below.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value         = messageText,
                onValueChange = { if (it.length <= MAX_CHARS) onTextChange(it) },
                modifier      = Modifier.fillMaxWidth().height(148.dp),
                placeholder   = {
                    Text(
                        text  = "e.g. Congratulations! You have won a special reward...",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = TealBright,
                    unfocusedBorderColor    = BorderColor,
                    focusedTextColor        = TextPrimary,
                    unfocusedTextColor      = TextPrimary,
                    cursorColor             = TealBright,
                    focusedContainerColor   = NavyCard,
                    unfocusedContainerColor = NavyCard
                ),
                shape    = RoundedCornerShape(12.dp),
                maxLines = 7,
                enabled  = !isAnalyzing
            )
            Row(
                modifier              = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text  = "$charCount / $MAX_CHARS",
                    color = if (charCount > MAX_CHARS * 0.9) RiskSuspicious else TextMuted,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick  = onAnalyze,
                enabled  = messageText.isNotBlank() && !isAnalyzing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = TealBright,
                    contentColor           = NavyDeep,
                    disabledContainerColor = NavyCard,
                    disabledContentColor   = TextMuted
                )
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(22.dp),
                        color       = TealBright,
                        strokeCap   = StrokeCap.Round,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text  = "Sending to backend...",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                } else {
                    Text(text = "\uD83D\uDEE1\uFE0F", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text  = if (messageText.isBlank()) "Enter a message above" else "Analyze Message",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (messageText.isBlank()) TextMuted else NavyDeep
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SAMPLE MESSAGE CHIPS
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun SampleMessageRow(onSampleSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text          = "Try a sample:",
            color         = TextSecondary,
            style         = MaterialTheme.typography.labelMedium,
            letterSpacing = 0.6.sp
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SampleChip(
                label   = "\u2713  Safe",
                color   = RiskSafe,
                bgColor = RiskSafeBg,
                modifier = Modifier.weight(1f),
                onClick = { onSampleSelected(SAMPLE_SAFE) }
            )
            SampleChip(
                label   = "\u26A0  Suspicious",
                color   = RiskSuspicious,
                bgColor = RiskSuspiciousBg,
                modifier = Modifier.weight(1f),
                onClick = { onSampleSelected(SAMPLE_SUSPICIOUS) }
            )
            SampleChip(
                label   = "\u2717  High Risk",
                color   = RiskHigh,
                bgColor = RiskHighBg,
                modifier = Modifier.weight(1f),
                onClick = { onSampleSelected(SAMPLE_HIGH_RISK) }
            )
        }
    }
}

@Composable
private fun SampleChip(
    label   : String,
    color   : Color,
    bgColor : Color,
    modifier: Modifier = Modifier,
    onClick : () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Text(
            text      = label,
            color     = color,
            style     = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines  = 1
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// RESULT SECTION — dispatches to idle / loading / error / success
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ResultSection(state: ScreenState) {
    AnimatedContent(
        targetState   = state,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
        label         = "resultState"
    ) { currentState ->
        when (currentState) {
            is ScreenState.Idle    -> EmptyResultHint()
            is ScreenState.Loading -> LoadingCard()
            is ScreenState.Error   -> ErrorCard(message = currentState.message)
            is ScreenState.Success -> ResultCard(result = currentState.result)
        }
    }
}

// ── Empty hint ────────────────────────────────────────────────────────────────

@Composable
private fun EmptyResultHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = NavySurface),
        border   = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "\uD83D\uDD0D", fontSize = 40.sp)
            Text(
                text      = stringResource(R.string.result_empty_title),
                color     = TextPrimary,
                style     = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text      = stringResource(R.string.result_empty_hint),
                color     = TextSecondary,
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = NavySurface),
        border   = androidx.compose.foundation.BorderStroke(1.dp, TealBright.copy(alpha = 0.3f))
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                color       = TealBright,
                strokeCap   = StrokeCap.Round,
                strokeWidth = 3.dp,
                modifier    = Modifier.size(52.dp)
            )
            Text(
                text  = stringResource(R.string.loading_sending),
                color = TealBright,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text      = stringResource(R.string.loading_awaiting),
                color     = TextSecondary,
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Error (backend unavailable or validation failed) ─────────────────────────

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = NavySurface),
        border   = androidx.compose.foundation.BorderStroke(1.dp, RiskHigh.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "\u26A0\uFE0F", fontSize = 26.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text  = stringResource(R.string.error_backend_title),
                    color = RiskHigh,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text  = message,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NavyCard)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text  = stringResource(R.string.error_backend_hint),
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text  = "cd D:\\Hacktopia",
                        color = TealBright,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text  = "uvicorn backend.main:app --host 0.0.0.0 --port 8000 --reload",
                        color = TealBright,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ── Success / Result ──────────────────────────────────────────────────────────

@Composable
private fun ResultCard(result: AnalysisResult) {
    val meta = riskMetaFor(result.riskLevel)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = NavySurface),
        border   = androidx.compose.foundation.BorderStroke(1.5.dp, meta.color.copy(alpha = 0.55f))
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backend source badge (replaces demo badge)
            BackendBadge(interactionId = result.interactionId)

            // Risk level banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(meta.bgColor)
                    .padding(16.dp)
            ) {
                Text(text = meta.emoji, fontSize = 34.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text  = stringResource(R.string.results_risk_level),
                        color = meta.color.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text  = localizedRiskLabel(meta.label),
                        color = meta.color,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Score + progress bar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = stringResource(R.string.result_risk_score),
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text  = "${result.score} / 100",
                        color = meta.color,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                LinearProgressIndicator(
                    progress   = { (result.score / 100f).coerceIn(0f, 1f) },
                    modifier   = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color      = meta.color,
                    trackColor = NavyCard,
                    strokeCap  = StrokeCap.Round
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.legend_safe),        color = RiskSafe,       style = MaterialTheme.typography.labelMedium)
                    Text(text = stringResource(R.string.legend_suspicious),  color = RiskSuspicious, style = MaterialTheme.typography.labelMedium)
                    Text(text = stringResource(R.string.legend_high_risk),  color = RiskHigh,       style = MaterialTheme.typography.labelMedium)
                }
            }

            SectionDivider()

            // Detection reasons from backend
            if (result.reasons.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text  = stringResource(R.string.result_why_flagged),
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    result.reasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier          = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavyCard)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text       = "\u2022",
                                color      = meta.color,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier.padding(end = 10.dp, top = 1.dp)
                            )
                            Text(text = localizedReason(reason), color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Signals row (compact)
            if (result.signals.isNotEmpty()) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = stringResource(R.string.result_signals_label),
                        color = TextMuted,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    result.signals.take(4).forEach { signal ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(meta.color.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text  = localizedSignal(signal),
                                color = meta.color,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            SectionDivider()

            // Recommended action
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text  = stringResource(R.string.result_recommended_action),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(meta.color.copy(alpha = 0.08f))
                        .border(1.dp, meta.color.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text     = "\uD83D\uDEE1\uFE0F",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 1.dp, end = 10.dp)
                    )
                    Text(
                        text       = localizedRecommendation(result.recommendedAction),
                        color      = TextPrimary,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Risk level label (resolved to localized string for the banner)
            // Note: meta.label is now a code like __HIGH_RISK__; resolve it here
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// BACKEND BADGE — replaces the v2 "DEMO RESULT" banner
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun BackendBadge(interactionId: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TealGlow)
            .border(1.dp, TealBright.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = "\u2705", fontSize = 13.sp, modifier = Modifier.padding(end = 6.dp))
        Column {
            Text(
                text  = "LIVE RESULT \u2014 ScamShield FastAPI backend (port 8000)",
                color = TealBright,
                style = MaterialTheme.typography.labelMedium
            )
            if (interactionId.isNotEmpty()) {
                Text(
                    text  = "ID: ${interactionId.take(18)}...",
                    color = TealBright.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// RECENT SCANS ROW
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun RecentScanRow(record: ScanRecord) {
    val meta = riskMetaFor(record.result.riskLevel)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = NavySurface),
        border   = androidx.compose.foundation.BorderStroke(1.dp, meta.color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(meta.bgColor)
            ) {
                Text(text = meta.emoji, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = record.preview,
                    color    = TextPrimary,
                    style    = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = "${meta.chipLabel}  \u2022  Score ${record.result.score}/100",
                    color = meta.color,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text  = record.timestamp,
                color = TextMuted,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SHARED UI PRIMITIVES
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        color         = TextMuted,
        style         = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun SectionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BorderColor)
    )
}

@Composable
private fun LanguageSelector() {
    val context = LocalContext.current
    val currentLang = remember { LanguagePrefs.getLanguage(context) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        val languages = listOf("en" to "English", "hi" to "हिन्दी", "kn" to "ಕನ್ನಡ")
        languages.forEach { (code, label) ->
            androidx.compose.material3.TextButton(
                onClick = {
                    if (code != currentLang) {
                        LanguagePrefs.setLanguage(context, code)
                        (context as? android.app.Activity)?.recreate()
                    }
                }
            ) {
                Text(
                    text = label, 
                    color = if (code == currentLang) TealBright else TextSecondary,
                    fontWeight = if (code == currentLang) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// AUTOMATIC SMS PROTECTION SECTION
// ═════════════════════════════════════════════════════════════════════════════

/**
 * AutoSmsProtectionSection
 * ─────────────────────────────────────────────────────────────────────────────
 * Shows the SMS auto-protection toggle and all required user-facing information:
 *  - What the feature does and how it works.
 *  - Privacy implications (preview text sent to local backend, not stored).
 *  - A button to open Android's Notification Access settings.
 *  - Clear warning that this is a LOCAL PROTOTYPE only.
 *  - Notification permission status.
 *
 * The toggle and the "Open Settings" button are the only two interactive elements.
 */
@Composable
fun AutoSmsProtectionSection() {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Persist toggle state via SharedPreferences.
    var isEnabled       by remember { mutableStateOf(SmsProtectionPrefs.isEnabled(context)) }
    // Re-check notification access each time the screen becomes RESUMED
    // (user may have just come back from the Settings screen).
    var listenerGranted by remember { mutableStateOf(SmsNotificationListenerService.isListenerEnabled(context)) }
    var notifGranted    by remember { mutableStateOf(ScamShieldNotifier.canPostNotifications(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerGranted = SmsNotificationListenerService.isListenerEnabled(context)
                notifGranted    = ScamShieldNotifier.canPostNotifications(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = NavySurface),
        border   = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isEnabled && listenerGranted) TealBright.copy(alpha = 0.4f) else BorderColor
        )
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Header row with toggle ──────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = "\uD83D\uDCF1 Auto SMS Protection",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text  = "Watches SMS notifications for scam patterns",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked         = isEnabled,
                    onCheckedChange = { checked ->
                        isEnabled = checked
                        SmsProtectionPrefs.setEnabled(context, checked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = NavyDeep,
                        checkedTrackColor   = TealBright,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = NavyCard
                    )
                )
            }

            SectionDivider()

            // ── Prototype disclaimer ────────────────────────────────────────
            SmsPrototypeDisclaimerBadge()

            // ── How it works ────────────────────────────────────────────────
            SmsInfoRow(
                emoji = "\u2139\uFE0F",
                text  = "When enabled, ScamShield monitors notifications from your default SMS app only. " +
                        "WhatsApp, Telegram and other apps are never scanned."
            )

            // ── Privacy statement ───────────────────────────────────────────
            SmsInfoRow(
                emoji = "\uD83D\uDD12",
                text  = "Only the short preview visible in your notification bar is sent " +
                        "to the ScamShield backend running on your own computer. " +
                        "No message content, phone numbers, or OTPs are stored on device."
            )

            // ── Permissions status ──────────────────────────────────────────
            SmsPermissionStatusRow(
                label   = "Notification Access",
                granted = listenerGranted
            )
            SmsPermissionStatusRow(
                label   = "Post Notifications (warnings)",
                granted = notifGranted
            )

            // ── Open Notification Access settings ───────────────────────────
            if (!listenerGranted) {
                Button(
                    onClick  = {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = TealBright,
                        contentColor   = NavyDeep
                    )
                ) {
                    Text(
                        text  = "\u2699\uFE0F  Open Notification Access Settings",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Text(
                    text  = "Tap the button above, find ScamShield in the list, and enable it. " +
                            "Then come back to this screen.",
                    color = RiskSuspicious,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // ── Disabled state message ──────────────────────────────────────
            if (!isEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NavyCard)
                        .padding(12.dp)
                ) {
                    Text(
                        text  = "Toggle is OFF \u2014 no SMS notifications will be analysed.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// ── Prototype badge ───────────────────────────────────────────────────────────

@Composable
private fun SmsPrototypeDisclaimerBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(RiskHighBg)
            .border(1.dp, RiskHigh.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = "\u26A0\uFE0F", fontSize = 13.sp, modifier = Modifier.padding(end = 6.dp))
        Text(
            text  = "LOCAL PROTOTYPE \u2014 Not eligible for Google Play Store distribution. " +
                    "Requires user-granted Notification Access. No RECEIVE_SMS permission used.",
            color = RiskHigh,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

// ── Info row ──────────────────────────────────────────────────────────────────

@Composable
private fun SmsInfoRow(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NavyCard)
            .padding(12.dp)
    ) {
        Text(text = emoji, fontSize = 16.sp, modifier = Modifier.padding(end = 10.dp, top = 1.dp))
        Text(text = text, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Permission status row ─────────────────────────────────────────────────────

@Composable
private fun SmsPermissionStatusRow(label: String, granted: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.fillMaxWidth()
    ) {
        Text(
            text     = if (granted) "\u2705" else "\u274C",
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text     = label,
            color    = if (granted) TextPrimary else RiskSuspicious,
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text  = if (granted) "Granted" else "Not granted",
            color = if (granted) RiskSafe else RiskSuspicious,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

