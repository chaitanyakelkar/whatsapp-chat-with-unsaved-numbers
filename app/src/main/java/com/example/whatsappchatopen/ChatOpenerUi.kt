package com.example.whatsappchatopen

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatsappchatopen.PhoneNumbers.Status

/** The whole screen, driven purely by its parameters so it can be previewed and tested. */
@Composable
fun ChatOpenerContent(
    country: PhoneNumbers.Country,
    input: TextFieldValue,
    check: PhoneNumbers.Check,
    sourceLabel: String?,
    autoOpen: Boolean,
    onInputChange: (TextFieldValue) -> Unit,
    onClear: () -> Unit,
    onCountryClick: () -> Unit,
    onOpen: () -> Unit,
    onPaste: () -> Unit,
    onAutoOpenChange: (Boolean) -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { BottomActionBar(waMeNumber = check.waMeNumber, onOpen = onOpen, onPaste = onPaste) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(32.dp))
            Header()
            Spacer(Modifier.height(32.dp))

            CountryField(country = country, onClick = onCountryClick)
            Spacer(Modifier.height(12.dp))
            PhoneField(
                value = input,
                onValueChange = onInputChange,
                onClear = onClear,
                onDone = onOpen,
                country = country,
                check = check,
            )
            SourcePill(sourceLabel, Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(24.dp))
            SettingsCard(autoOpen = autoOpen, onAutoOpenChange = onAutoOpenChange)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun Header(modifier: Modifier = Modifier) {
    Column(modifier) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_chat_add),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("Message any number", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Start a WhatsApp chat without saving the contact first.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Read-only field that opens the country picker. */
@Composable
fun CountryField(country: PhoneNumbers.Country, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        OutlinedTextField(
            value = country.name,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Country or region") },
            leadingIcon = { Text(country.flag, fontSize = 22.sp) },
            suffix = { Text("+${country.callingCode}", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = { Icon(painterResource(R.drawable.ic_expand_more), contentDescription = null) },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        // Transparent overlay so the whole field behaves as one button (the text field would eat the click).
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 8.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClickLabel = "Change country", role = Role.DropdownList, onClick = onClick)
        )
    }
}

@Composable
fun PhoneField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
    country: PhoneNumbers.Country,
    check: PhoneNumbers.Check,
    modifier: Modifier = Modifier,
) {
    val lengths = remember(country.regionCode) { PhoneNumbers.possibleLengths(country.regionCode) }
    val isError = check.status == Status.TOO_LONG
    val isValid = check.status == Status.VALID
    val supportingColor by animateColorAsState(
        when {
            isError -> MaterialTheme.colorScheme.error
            isValid -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "supportingColor",
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text("Phone number") },
        placeholder = { Text("Enter number") },
        prefix = { Text("+${country.callingCode} ", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, letterSpacing = 1.sp),
        shape = MaterialTheme.shapes.medium,
        isError = isError,
        trailingIcon = if (value.text.isNotEmpty()) {
            {
                IconButton(onClick = onClear) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Clear number")
                }
            }
        } else null,
        supportingText = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isValid || isError) {
                    Icon(
                        painterResource(if (isValid) R.drawable.ic_check_circle else R.drawable.ic_error),
                        contentDescription = null,
                        tint = supportingColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(statusText(check, country, lengths), color = supportingColor, modifier = Modifier.weight(1f))
                if (check.digitCount > 0 && lengths.isNotEmpty()) {
                    Text("${check.digitCount}/${lengths.max()}", color = supportingColor)
                }
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onDone() }),
    )
}

private fun statusText(check: PhoneNumbers.Check, country: PhoneNumbers.Country, lengths: List<Int>): String {
    val expected = PhoneNumbers.describeLengths(lengths)
    return when (check.status) {
        Status.EMPTY -> "${country.name} mobile numbers have $expected"
        Status.TOO_SHORT, Status.WRONG_LENGTH -> "${country.name} numbers have $expected"
        Status.TOO_LONG -> "Too long for ${country.name}"
        Status.UNLIKELY -> "Unusual for ${country.name}, but you can still try"
        Status.VALID -> "Looks good"
    }
}

/** Small tonal pill telling the user where a number came from ("From clipboard"). */
@Composable
fun SourcePill(text: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = text != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        // Keep showing the last text while animating out.
        var lastText by remember { mutableStateOf(text.orEmpty()) }
        if (text != null) lastText = text
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_paste),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    lastText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
fun SettingsCard(autoOpen: Boolean, onAutoOpenChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        ListItem(
            modifier = Modifier.toggleable(value = autoOpen, role = Role.Switch, onValueChange = onAutoOpenChange),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = { IconBadge(R.drawable.ic_bolt) },
            headlineContent = { Text("Open automatically") },
            supportingContent = { Text("Skip the button when a valid number is pasted, shared or copied") },
            trailingContent = { Switch(checked = autoOpen, onCheckedChange = null) },
        )
    }
}

@Composable
private fun IconBadge(@DrawableRes icon: Int) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Primary action pinned to the bottom, within thumb reach and above the keyboard. */
@Composable
fun BottomActionBar(
    waMeNumber: String?,
    onOpen: () -> Unit,
    onPaste: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (waMeNumber != null) "wa.me/$waMeNumber" else " ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onPaste,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(R.drawable.ic_paste),
                            contentDescription = "Paste from clipboard",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Button(
                    onClick = onOpen,
                    enabled = waMeNumber != null,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_send), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Open in WhatsApp", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPickerSheet(
    countries: List<PhoneNumbers.Country>,
    selected: PhoneNumbers.Country,
    onSelect: (PhoneNumbers.Country) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        CountryPickerContent(countries, selected, onSelect)
    }
}

/** Title, search bar and country list shown inside [CountryPickerSheet]. */
@Composable
fun CountryPickerContent(
    countries: List<PhoneNumbers.Country>,
    selected: PhoneNumbers.Country,
    onSelect: (PhoneNumbers.Country) -> Unit,
    initialQuery: String = "",
) {
    var query by remember { mutableStateOf(initialQuery) }
    val filtered = remember(query) { countries.filter { it.matches(query) } }

    Column {
        Text(
            "Choose country",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        )
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search name or code") },
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = "Clear search")
                    }
                }
            } else null,
            singleLine = true,
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        ) {
            if (query.isBlank()) {
                item(key = "selected") {
                    SectionLabel("Selected")
                    CountryRow(selected, isSelected = true, onClick = { onSelect(selected) })
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    SectionLabel("All countries")
                }
            }
            items(filtered, key = { it.regionCode }) { c ->
                CountryRow(c, isSelected = c.regionCode == selected.regionCode, onClick = { onSelect(c) })
            }
            if (filtered.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "No countries match “$query”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                    )
                }
            }
            item(key = "bottom-inset") { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun CountryRow(country: PhoneNumbers.Country, isSelected: Boolean, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        ),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Text(country.flag, fontSize = 22.sp)
            }
        },
        headlineContent = {
            Text(country.name, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "+${country.callingCode}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isSelected) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painterResource(R.drawable.ic_check),
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
    )
}
