package com.example.whatsappchatopen

import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.edit
import androidx.core.net.toUri
import com.example.whatsappchatopen.ui.theme.WhatsappChatopenTheme
import kotlinx.coroutines.launch

private const val PREFS = "settings"
private const val DEFAULT_REGION = "IN"
private const val KEY_REGION = "selected_region"
private const val KEY_AUTO_OPEN = "auto_open"
private const val KEY_LAST_AUTO_OPENED_CLIP = "last_auto_opened_clip"

class MainActivity : ComponentActivity() {

    /** Text handed to us via "Share" or the text-selection menu, waiting to be consumed by the UI. */
    private val sharedText = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) sharedText.value = intent.sharedText()
        setContent {
            WhatsappChatopenTheme {
                ChatOpenerScreen(
                    sharedText = sharedText.value,
                    onSharedTextConsumed = { sharedText.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.sharedText()?.let { sharedText.value = it }
    }
}

private fun Intent.sharedText(): String? = when (action) {
    Intent.ACTION_SEND -> getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
    Intent.ACTION_PROCESS_TEXT -> getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
    else -> null
}

@Composable
fun ChatOpenerScreen(sharedText: String?, onSharedTextConsumed: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val countries = remember { PhoneNumbers.countries() }

    var region by rememberSaveable {
        mutableStateOf(prefs.getString(KEY_REGION, null)?.takeIf(PhoneNumbers::isSupported) ?: DEFAULT_REGION)
    }
    var input by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var autoOpen by rememberSaveable { mutableStateOf(prefs.getBoolean(KEY_AUTO_OPEN, false)) }
    var showCountryPicker by rememberSaveable { mutableStateOf(false) }
    // Last clipboard text we looked at, so coming back from WhatsApp doesn't refill the box.
    var lastSeenClip by rememberSaveable { mutableStateOf<String?>(null) }
    var skipNextClipboardRead by remember { mutableStateOf(false) }
    // Where the current number came from ("From clipboard"), shown as a pill under the field.
    var sourceLabel by rememberSaveable { mutableStateOf<String?>(null) }

    val country = remember(region) { PhoneNumbers.country(region) }
    val check = remember(region, input.text) { PhoneNumbers.check(region, input.text) }

    LaunchedEffect(region) { prefs.edit { putString(KEY_REGION, region) } }

    fun message(text: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }

    fun openChat(waMeNumber: String) {
        if (!openWhatsAppChat(context, waMeNumber)) message("No app found to open WhatsApp links")
    }

    /** Puts a detected number in the box (country selected, "+code" removed) and opens it if enabled. */
    fun useNumber(found: PhoneNumbers.Extracted, source: String?, allowAutoOpen: Boolean): Boolean {
        region = found.regionCode
        input = TextFieldValue(found.nationalNumber, TextRange(found.nationalNumber.length))
        sourceLabel = source
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        val waMeNumber = PhoneNumbers.check(found.regionCode, found.nationalNumber).waMeNumber
        if (autoOpen && allowAutoOpen && waMeNumber != null) {
            openChat(waMeNumber)
            return true
        }
        return false
    }

    // Share / "Chat on Whatsapp" from the text-selection menu.
    LaunchedEffect(sharedText) {
        val text = sharedText ?: return@LaunchedEffect
        onSharedTextConsumed()
        skipNextClipboardRead = true
        val found = PhoneNumbers.extract(text, region, strict = false)
        if (found != null) useNumber(found, "From shared text", allowAutoOpen = true)
        else message("No phone number found in the shared text")
    }

    // Android only lets apps read the clipboard while they have window focus, so check each time we get it.
    // Only the current clipboard entry is read; clipboard-history apps keep older entries to themselves.
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(windowFocused) {
        if (!windowFocused) return@LaunchedEffect
        val clip = readClipboardText(context) ?: return@LaunchedEffect
        if (clip == lastSeenClip) return@LaunchedEffect
        lastSeenClip = clip
        if (skipNextClipboardRead) {
            skipNextClipboardRead = false
            return@LaunchedEffect
        }
        val found = PhoneNumbers.extract(clip, region, strict = true) ?: return@LaunchedEffect
        // Don't auto-open the same copied number again every time the app is relaunched.
        val isNewClip = prefs.getString(KEY_LAST_AUTO_OPENED_CLIP, null) != clip
        if (useNumber(found, "From clipboard", allowAutoOpen = isNewClip)) {
            prefs.edit { putString(KEY_LAST_AUTO_OPENED_CLIP, clip) }
        }
    }

    fun onInputChange(new: TextFieldValue) {
        val inserted = insertedText(input.text, new.text)
        // Several characters arriving at once: a paste, autofill or keyboard suggestion.
        val pasted = inserted.count(Char::isDigit) >= 4
        val found = when {
            pasted -> PhoneNumbers.extract(inserted, region, strict = false)
                ?: PhoneNumbers.extract(new.text, region, strict = false)
            // Typed "+<country code>…" by hand: switch country as soon as the number is complete.
            new.text.contains('+') -> PhoneNumbers.extract(new.text, region, strict = true)
            else -> null
        }
        if (found != null) {
            useNumber(found, if (pasted) "Pasted" else null, allowAutoOpen = pasted)
        } else {
            sourceLabel = null
            val filtered = new.text.filter { it.isDigit() || it in "+ -()." }
            input = if (filtered == new.text) new else TextFieldValue(filtered, TextRange(filtered.length))
        }
    }

    ChatOpenerContent(
        country = country,
        input = input,
        check = check,
        sourceLabel = sourceLabel,
        autoOpen = autoOpen,
        snackbar = snackbar,
        onInputChange = ::onInputChange,
        onClear = {
            input = TextFieldValue()
            sourceLabel = null
        },
        onCountryClick = { showCountryPicker = true },
        onOpen = { check.waMeNumber?.let(::openChat) },
        onPaste = {
            val clip = readClipboardText(context)
            val found = clip?.let { PhoneNumbers.extract(it, region, strict = false) }
            if (clip != null) lastSeenClip = clip
            if (found != null) useNumber(found, "From clipboard", allowAutoOpen = true)
            else message("Clipboard doesn't contain a phone number")
        },
        onAutoOpenChange = {
            autoOpen = it
            prefs.edit { putBoolean(KEY_AUTO_OPEN, it) }
        },
    )

    if (showCountryPicker) {
        CountryPickerSheet(
            countries = countries,
            selected = country,
            onSelect = {
                region = it.regionCode
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false },
        )
    }
}

/** The text that was added between [old] and [new] (the pasted part). */
private fun insertedText(old: String, new: String): String {
    val prefix = old.commonPrefixWith(new).length
    val suffix = old.substring(prefix).commonSuffixWith(new.substring(prefix)).length
    return new.substring(prefix, new.length - suffix)
}

private val whatsAppPackages = listOf("com.whatsapp", "com.whatsapp.w4b") // WhatsApp, WhatsApp Business

/**
 * Opens the chat directly in the WhatsApp app. Android 12+ sends plain https links to the browser
 * unless the app is verified for that domain, so the link is addressed to WhatsApp explicitly and
 * the browser is only used when WhatsApp isn't installed.
 */
private fun openWhatsAppChat(context: Context, waMeNumber: String): Boolean {
    val waMeLink = "https://wa.me/$waMeNumber".toUri()
    val candidates = whatsAppPackages.flatMap { pkg ->
        listOf(
            Intent(Intent.ACTION_VIEW, waMeLink).setPackage(pkg),
            Intent(Intent.ACTION_VIEW, "whatsapp://send?phone=$waMeNumber".toUri()).setPackage(pkg),
        )
    } + Intent(Intent.ACTION_VIEW, waMeLink)
    return candidates.any { intent ->
        try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}

private fun readClipboardText(context: Context): String? = try {
    val clip = context.getSystemService(ClipboardManager::class.java)?.primaryClip
    if (clip == null || clip.itemCount == 0) null
    else clip.getItemAt(0).coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
} catch (_: SecurityException) {
    null
}
