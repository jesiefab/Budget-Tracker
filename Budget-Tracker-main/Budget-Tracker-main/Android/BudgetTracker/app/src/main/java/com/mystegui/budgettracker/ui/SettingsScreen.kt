package com.mystegui.budgettracker.ui

import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mystegui.budgettracker.AppState
import com.mystegui.budgettracker.BudgetViewModel
import com.mystegui.budgettracker.ReminderManager
import com.mystegui.budgettracker.ReminderReceiver
import com.mystegui.budgettracker.ui.theme.AppColors
import com.mystegui.budgettracker.ui.theme.LocalAppColors

private const val ADMIN_PASSWORD = "Admin_Tools"

val THEMES = listOf("Dark", "Light", "Midnight Blue", "Forest Green", "Warm Sunset")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: AppState, viewModel: BudgetViewModel) {
    val colors = LocalAppColors.current
    val context = LocalContext.current

    var themeExpanded by remember { mutableStateOf(false) }

    var password by remember { mutableStateOf("") }
    var adminUnlocked by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf("") }

    var showConfirm by remember { mutableStateOf(false) }
    var confirmMessage by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf({}) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = colors.card,
            title = {
                Text(
                    "Confirm",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(confirmMessage, color = colors.textMuted, fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmAction()
                        showConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text("Yes", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirm = false }) {
                    Text("Cancel", color = colors.textMuted)
                }
            }
        )
    }

    fun confirm(message: String, action: () -> Unit) {
        confirmMessage = message
        confirmAction = action
        showConfirm = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Reminder Card ──────────────────────────────────────────────────────
        item {
            ReminderCard(colors = colors)
        }

        // ── Theme Card ─────────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Appearance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.accent
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Change the app color theme. Applies instantly.",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(12.dp))

                    Text("Theme", fontSize = 12.sp, color = colors.textMuted)
                    Spacer(Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = themeExpanded,
                        onExpandedChange = { themeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = state.theme,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = themeExpanded
                                )
                            },
                            colors = outlinedTextFieldColors(colors),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false },
                            modifier = Modifier.background(colors.card)
                        ) {
                            THEMES.forEach { theme ->
                                DropdownMenuItem(
                                    text = { Text(theme, color = colors.textPrimary) },
                                    onClick = {
                                        viewModel.setTheme(theme)
                                        themeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Admin Auth Card ────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Admin Access",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.accent
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Enter the admin password to unlock developer tools.",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(12.dp))

                    if (!adminUnlocked) {
                        Text("Password", fontSize = 12.sp, color = colors.textMuted)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = {
                                Text("Enter password", color = colors.textMuted)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = outlinedTextFieldColors(colors),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (authError.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(authError, color = colors.danger, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (password == ADMIN_PASSWORD) {
                                    adminUnlocked = true
                                    authError = ""
                                    password = ""
                                } else {
                                    authError = "Wrong password."
                                    password = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Unlock",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (adminUnlocked) "Status: Unlocked" else "Status: Locked",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (adminUnlocked) colors.success else colors.danger
                        )
                    } else {
                        // ── Admin Tools ────────────────────────────────────────
                        Text(
                            "Developer Tools",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.warning
                        )
                        Spacer(Modifier.height(12.dp))

                        AdminResetCard(
                            title = "Reset Savings Progress",
                            desc = "Clears savings amount and resets goal to ₱1,000.",
                            buttonLabel = "Reset Savings",
                            buttonColor = colors.warning,
                            onConfirm = {
                                confirm(
                                    "Reset savings to zero and goal to ₱1,000?"
                                ) {
                                    viewModel.adminResetSavings()
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        AdminResetCard(
                            title = "Reset XP & Level",
                            desc = "Wipes all XP and sets level back to 1.",
                            buttonLabel = "Reset XP",
                            buttonColor = colors.warning,
                            onConfirm = {
                                confirm("Reset all XP and level back to 1?") {
                                    viewModel.adminResetXP()
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        AdminResetCard(
                            title = "Reset Savings + XP",
                            desc = "Clears savings, goal, XP, and level all at once.",
                            buttonLabel = "Full Savings Reset",
                            buttonColor = colors.danger,
                            onConfirm = {
                                confirm(
                                    "Clear savings, goal, XP and level? This can't be undone."
                                ) {
                                    viewModel.adminResetSavings()
                                    viewModel.adminResetXP()
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        AdminResetCard(
                            title = "Reset Everything",
                            desc = "Wipes all expenses, budget, savings, XP, and level.",
                            buttonLabel = "FULL APP RESET",
                            buttonColor = colors.danger,
                            onConfirm = {
                                confirm("Wipe ALL data? No undo. Really?") {
                                    viewModel.adminFullReset()
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        AdminResetCard(
                            title = "Test Notification",
                            desc = "Manually fires the daily reminder to test it works.",
                            buttonLabel = "Send Test Notification",
                            buttonColor = colors.accent,
                            onConfirm = {
                                confirm("Send a test notification now?") {
                                    val intent = Intent(context, ReminderReceiver::class.java)
                                    context.sendBroadcast(intent)
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { adminUnlocked = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, colors.danger
                            )
                        ) {
                            Text(
                                "Lock Admin",
                                color = colors.danger,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Admin Reset Card ──────────────────────────────────────────────────────────

@Composable
fun AdminResetCard(
    title: String,
    desc: String,
    buttonLabel: String,
    buttonColor: Color,
    onConfirm: () -> Unit
) {
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.bg),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, buttonColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = buttonColor
            )
            Spacer(Modifier.height(2.dp))
            Text(desc, fontSize = 12.sp, color = colors.textMuted)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    buttonLabel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ── Reminder Card ─────────────────────────────────────────────────────────────

@Composable
fun ReminderCard(colors: AppColors) {
    val context = LocalContext.current

    var reminderEnabled by remember {
        mutableStateOf(ReminderManager.isReminderEnabled(context))
    }
    var hour by remember { mutableStateOf(ReminderManager.getSavedHour(context)) }
    var minute by remember { mutableStateOf(ReminderManager.getSavedMinute(context)) }

    fun formatTime(h: Int, m: Int): String {
        val amPm = if (h < 12) "AM" else "PM"
        val displayHour = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else   -> h
        }
        return "$displayHour:${m.toString().padStart(2, '0')} $amPm"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Daily Reminder",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Get a daily nudge to log your expenses.",
                fontSize = 12.sp,
                color = colors.textMuted
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Enable Reminder",
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { enabled ->
                        reminderEnabled = enabled
                        ReminderManager.setReminderEnabled(context, enabled)
                        if (enabled) {
                            ReminderManager.scheduleReminder(context, hour, minute)
                        } else {
                            ReminderManager.cancelReminder(context)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = colors.accent,
                        checkedTrackColor   = colors.accent.copy(alpha = 0.4f),
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.border
                    )
                )
            }

            if (reminderEnabled) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Reminder Time",
                            fontSize = 14.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            formatTime(hour, minute),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                    }
                    Button(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, selectedHour, selectedMinute ->
                                    hour = selectedHour
                                    minute = selectedMinute
                                    ReminderManager.scheduleReminder(
                                        context, selectedHour, selectedMinute
                                    )
                                },
                                hour, minute, false
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Change Time",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}