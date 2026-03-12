package inc.flide.vim8.app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import inc.flide.vim8.R
import inc.flide.vim8.datastore.model.observeAsState
import inc.flide.vim8.datastore.ui.PreferenceGroup
import inc.flide.vim8.datastore.ui.SwitchPreference
import inc.flide.vim8.lib.compose.Screen
import inc.flide.vim8.lib.compose.stringRes
import inc.flide.vim8.textReplacementManager

@Composable
fun TextReplacementScreen() = Screen {
    title = stringRes(R.string.settings__text_replacement__title)

    val context = LocalContext.current
    val manager by context.textReplacementManager()

    // Dialog state lives in the @Composable builder scope so both the FAB
    // (registered via floatingActionButton{}) and the AlertDialog can access it.
    var showDialog by remember { mutableStateOf(false) }
    var dialogAbbreviation by remember { mutableStateOf("") }
    var dialogExpansion by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    // FAB registered through the Screen DSL to appear in the Scaffold's FAB slot.
    floatingActionButton {
        FloatingActionButton(
            onClick = {
                dialogAbbreviation = ""
                dialogExpansion = ""
                dialogError = null
                showDialog = true
            }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringRes(R.string.settings__text_replacement__add_entry)
            )
        }
    }

    // AlertDialog rendered directly in the @Composable builder scope (outside the
    // Scaffold, but still within the composable tree).
    if (showDialog) {
        // Hoist composable string lookups into the composable lambda scope —
        // they cannot be called from inside the non-composable onClick lambda.
        val blankAbbreviationError =
            stringRes(R.string.settings__text_replacement__error_blank_abbreviation)

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringRes(R.string.settings__text_replacement__add_entry)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dialogAbbreviation,
                        onValueChange = {
                            dialogAbbreviation = it
                            dialogError = null
                        },
                        label = {
                            Text(stringRes(R.string.settings__text_replacement__abbreviation_label))
                        },
                        placeholder = {
                            Text(stringRes(R.string.settings__text_replacement__abbreviation_placeholder))
                        },
                        singleLine = true,
                        isError = dialogError != null,
                        supportingText = if (dialogError != null) {
                            { Text(dialogError!!) }
                        } else {
                            null
                        }
                    )
                    OutlinedTextField(
                        value = dialogExpansion,
                        onValueChange = { dialogExpansion = it },
                        label = {
                            Text(stringRes(R.string.settings__text_replacement__expansion_label))
                        },
                        placeholder = {
                            Text(stringRes(R.string.settings__text_replacement__expansion_placeholder))
                        },
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (dialogAbbreviation.isBlank()) {
                        // Use hoisted string — not a composable call here.
                        dialogError = blankAbbreviationError
                    } else {
                        manager.addEntry(dialogAbbreviation.trim(), dialogExpansion)
                        showDialog = false
                    }
                }) {
                    Text(stringRes(R.string.settings__text_replacement__save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringRes(R.string.settings__text_replacement__cancel))
                }
            }
        )
    }

    content {
        // observeAsState requires a composable scope; prefs is in scope here.
        val rawEntries by prefs.textReplacement.entries.observeAsState()
        val entries = remember(rawEntries) { manager.getEntries() }

        // Enable/disable toggle
        PreferenceGroup {
            SwitchPreference(
                pref = prefs.textReplacement.enabled,
                title = stringRes(R.string.settings__text_replacement__enabled)
            )
        }

        // Trigger hint
        Text(
            text = stringRes(R.string.settings__text_replacement__trigger_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Empty-state label
        if (entries.isEmpty()) {
            Text(
                text = stringRes(R.string.settings__text_replacement__empty_list),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp)
            )
        } else {
            // Plain Column — LazyColumn cannot be nested inside PreferenceLayout's
            // vertical scroll. The list of abbreviations is expected to be small.
            // Use a for-loop (not forEach) so Compose calls inside are allowed.
            Column(modifier = Modifier.fillMaxWidth()) {
                for ((abbreviation, expansion) in entries) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = abbreviation,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = expansion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { manager.removeEntry(abbreviation) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringRes(
                                        R.string.settings__text_replacement__delete_entry
                                    ),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
