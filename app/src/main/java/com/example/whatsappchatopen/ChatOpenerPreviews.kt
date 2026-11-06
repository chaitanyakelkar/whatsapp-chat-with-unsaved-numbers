package com.example.whatsappchatopen

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.whatsappchatopen.ui.theme.WhatsappChatopenTheme
import java.util.Locale

// Previews render with fixed English country names so they look the same on every machine.
private val previewCountries by lazy { PhoneNumbers.countries(Locale.ENGLISH) }
private fun previewCountry(region: String) = PhoneNumbers.country(region, Locale.ENGLISH)

/** Screen with [number] typed for [region], checked exactly like the real app does. */
@Composable
private fun ScreenPreview(region: String, number: String, sourceLabel: String? = null, autoOpen: Boolean = false) {
    WhatsappChatopenTheme {
        ChatOpenerContent(
            country = previewCountry(region),
            input = TextFieldValue(number, TextRange(number.length)),
            check = PhoneNumbers.check(region, number),
            sourceLabel = sourceLabel,
            autoOpen = autoOpen,
            onInputChange = {},
            onClear = {},
            onCountryClick = {},
            onOpen = {},
            onPaste = {},
            onAutoOpenChange = {},
        )
    }
}

// ---- Full screen ----

@Preview(name = "Empty", group = "Screen", showSystemUi = true)
@Composable
private fun ScreenEmptyPreview() = ScreenPreview(region = "IN", number = "")

@Preview(name = "Typing (too short)", group = "Screen", showSystemUi = true)
@Composable
private fun ScreenTypingPreview() = ScreenPreview(region = "IN", number = "98765")

@Preview(name = "Valid, from clipboard", group = "Screen", showSystemUi = true)
@Composable
private fun ScreenValidPreview() =
    ScreenPreview(region = "IN", number = "9876543210", sourceLabel = "From clipboard", autoOpen = true)

@Preview(name = "Too long", group = "Screen", showSystemUi = true)
@Composable
private fun ScreenTooLongPreview() = ScreenPreview(region = "GB", number = "74001234567890")

@Preview(name = "Dark, valid", group = "Screen", showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ScreenDarkPreview() = ScreenPreview(region = "US", number = "6502530000", sourceLabel = "Pasted")

@Preview(name = "Large text (200%)", group = "Screen", showSystemUi = true, fontScale = 2f)
@Composable
private fun ScreenLargeFontPreview() = ScreenPreview(region = "IN", number = "9876543210")

@Preview(name = "Small phone", group = "Screen", device = "spec:width=320dp,height=640dp,dpi=320")
@Composable
private fun ScreenSmallPhonePreview() = ScreenPreview(region = "SG", number = "8123")

@Preview(name = "Tablet", group = "Screen", device = Devices.PIXEL_TABLET)
@Composable
private fun ScreenTabletPreview() = ScreenPreview(region = "DE", number = "15123456789")

// ---- Country picker ----

@Composable
private fun PickerPreview(query: String = "") {
    WhatsappChatopenTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            CountryPickerContent(
                countries = previewCountries,
                selected = previewCountry("IN"),
                onSelect = {},
                initialQuery = query,
            )
        }
    }
}

@Preview(name = "Picker", group = "Country picker", heightDp = 700)
@Composable
private fun PickerDefaultPreview() = PickerPreview()

@Preview(name = "Picker, dark", group = "Country picker", heightDp = 700, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PickerDarkPreview() = PickerPreview()

@Preview(name = "Picker, searching \"uni\"", group = "Country picker", heightDp = 500)
@Composable
private fun PickerSearchPreview() = PickerPreview(query = "uni")

@Preview(name = "Picker, searching by code \"+44\"", group = "Country picker", heightDp = 400)
@Composable
private fun PickerCodeSearchPreview() = PickerPreview(query = "+44")

@Preview(name = "Picker, no results", group = "Country picker", heightDp = 300)
@Composable
private fun PickerEmptyPreview() = PickerPreview(query = "zzz")

// ---- Components ----

@Composable
private fun ComponentPreview(content: @Composable () -> Unit) {
    WhatsappChatopenTheme {
        Surface {
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}

@Preview(name = "Phone field states", group = "Components", widthDp = 380)
@Composable
private fun PhoneFieldStatesPreview() = ComponentPreview {
    listOf("IN" to "", "IN" to "98765", "IN" to "9876543210", "IN" to "98765432101234", "IN" to "123456789").forEach { (region, number) ->
        PhoneField(
            value = TextFieldValue(number),
            onValueChange = {},
            onClear = {},
            onDone = {},
            country = previewCountry(region),
            check = PhoneNumbers.check(region, number),
        )
    }
}

@Preview(name = "Country field", group = "Components", widthDp = 380)
@Composable
private fun CountryFieldPreview() = ComponentPreview {
    CountryField(country = previewCountry("GB"), onClick = {})
}

@Preview(name = "Settings card", group = "Components", widthDp = 380)
@Composable
private fun SettingsCardPreview() = ComponentPreview {
    SettingsCard(autoOpen = true, onAutoOpenChange = {})
    SettingsCard(autoOpen = false, onAutoOpenChange = {}, modifier = Modifier.padding(top = 12.dp))
}

@Preview(name = "Bottom bar, enabled", group = "Components", widthDp = 380)
@Composable
private fun BottomBarEnabledPreview() = WhatsappChatopenTheme {
    BottomActionBar(waMeNumber = "919876543210", onOpen = {}, onPaste = {})
}

@Preview(name = "Bottom bar, disabled", group = "Components", widthDp = 380)
@Composable
private fun BottomBarDisabledPreview() = WhatsappChatopenTheme {
    BottomActionBar(waMeNumber = null, onOpen = {}, onPaste = {})
}

@Preview(name = "Bottom bar, dark", group = "Components", widthDp = 380, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun BottomBarDarkPreview() = WhatsappChatopenTheme {
    BottomActionBar(waMeNumber = "447400123456", onOpen = {}, onPaste = {})
}

@Preview(name = "Source pill", group = "Components")
@Composable
private fun SourcePillPreview() = ComponentPreview {
    SourcePill("From clipboard")
}
