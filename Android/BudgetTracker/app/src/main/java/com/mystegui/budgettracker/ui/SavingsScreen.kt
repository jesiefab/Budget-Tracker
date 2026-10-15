package com.mystegui.budgettracker.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mystegui.budgettracker.AppState
import com.mystegui.budgettracker.BudgetViewModel
import com.mystegui.budgettracker.SavingsGoal
import com.mystegui.budgettracker.getTitle
import com.mystegui.budgettracker.ui.theme.LocalAppColors

@Composable
fun SavingsScreen(state: AppState, viewModel: BudgetViewModel, onToast: (String, Color) -> Unit) {
    val colors = LocalAppColors.current

    var goalInput      by remember { mutableStateOf("") }
    var depositInput   by remember { mutableStateOf("") }
    var withdrawInput  by remember { mutableStateOf("") }
    var goalNameInput  by remember { mutableStateOf("") }
    var goalTargetInput by remember { mutableStateOf("") }

    // Confirm dialog state
    var showConfirm    by remember { mutableStateOf(false) }
    var confirmMessage by remember { mutableStateOf("") }
    var confirmAction  by remember { mutableStateOf({}) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor   = colors.card,
            title = { Text("Confirm", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text  = { Text(confirmMessage, color = colors.textMuted, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = { confirmAction(); showConfirm = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) { Text("Yes", color = Color.White) }
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
        confirmAction  = action
        showConfirm    = true
    }

    val savingsPct = (state.savingsPct / 100f).coerceIn(0f, 1f)
    val xpPct      = (state.xpPct / 100f).coerceIn(0f, 1f)

    val motivationText = when {
        state.savingsPct >= 100f -> "Goal reached! Nice work."
        state.savingsPct >= 50f  -> "Halfway there! Keep going."
        state.savingsPct > 0f    -> "Every peso counts."
        else                     -> "Set a goal and start saving!"
    }
    val motivationColor = when {
        state.savingsPct >= 100f -> colors.success
        state.savingsPct >= 50f  -> colors.accent
        else                     -> colors.textMuted
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(colors.bg).padding(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Rank Card ──────────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = colors.card),
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Saver Rank", fontWeight = FontWeight.Bold,
                        fontSize = 13.sp, color = colors.warning)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column {
                            Text("LVL ${state.level}", fontWeight = FontWeight.Black,
                                fontSize = 34.sp, color = colors.textPrimary)
                            Text(getTitle(state.level), fontWeight = FontWeight.Bold,
                                fontSize = 16.sp, color = colors.warning)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("XP", fontSize = 11.sp, color = colors.textMuted)
                                Text("${state.lvlXP} / ${state.nextXP}",
                                    fontSize = 11.sp, color = colors.textMuted)
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress   = { xpPct },
                                modifier   = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color      = colors.warning,
                                trackColor = colors.border
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Total XP: ${state.totalXP}  •  +1 XP per ₱10 saved",
                                fontSize = 11.sp, color = colors.textMuted)
                        }
                    }
                }
            }
        }

        // ── Savings Progress Card ──────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = colors.card),
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Savings Goal", fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = colors.textPrimary)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress   = { savingsPct },
                        modifier   = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color      = colors.success,
                        trackColor = colors.border
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(motivationText, fontSize = 12.sp, color = motivationColor)
                        Text("${"%.0f".format(state.savingsPct)}%",
                            fontSize = 12.sp, color = colors.textMuted)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            label = "Saved", value = "₱${"%.2f".format(state.currentSavings)}",
                            valueColor = colors.success, cardColor = colors.card,
                            borderColor = colors.border, modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Still Needed", value = "₱${"%.2f".format(state.savingsNeeded)}",
                            valueColor = colors.warning, cardColor = colors.card,
                            borderColor = colors.border, modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ── Set Goal Card ──────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = colors.card),
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Set Goal", fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = colors.textPrimary)
                    Spacer(Modifier.height(10.dp))
                    Text("Goal Amount (₱)", fontSize = 12.sp, color = colors.textMuted)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value         = goalInput,
                        onValueChange = { goalInput = it },
                        placeholder   = { Text("${"%.2f".format(state.savingsGoal)}", color = colors.textMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors        = outlinedTextFieldColors(colors),
                        shape         = RoundedCornerShape(8.dp),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val g = goalInput.toDoubleOrNull() ?: return@Button
                            if (g <= 0) return@Button
                            viewModel.setSavingsGoal(g)
                            goalInput = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape    = RoundedCornerShape(8.dp)
                    ) { Text("Set Goal", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }

        // ── Add / Withdraw Savings Card ────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = colors.card),
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add / Withdraw Savings", fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = colors.textPrimary)
                    Spacer(Modifier.height(10.dp))

                    // Deposit
                    Text("Amount to Save (₱)", fontSize = 12.sp, color = colors.textMuted)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value         = depositInput,
                        onValueChange = { depositInput = it },
                        placeholder   = { Text("0.00", color = colors.textMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors        = outlinedTextFieldColors(colors),
                        shape         = RoundedCornerShape(8.dp),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val amt = depositInput.toDoubleOrNull() ?: return@Button
                            if (amt <= 0) return@Button
                            val prevLevel = state.level
                            val xpEarned  = viewModel.addSavings(amt)
                            val newLevel  = state.level
                            depositInput  = ""
                            if (newLevel > prevLevel) {
                                onToast("⬆ Level Up! Now ${getTitle(newLevel)}", colors.warning)
                            } else {
                                onToast("⚡ +$xpEarned XP earned!", colors.accent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = colors.success),
                        shape    = RoundedCornerShape(8.dp)
                    ) { Text("Add Savings  +XP", color = Color.White, fontWeight = FontWeight.Bold) }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = colors.border, thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))

                    // Withdraw
                    Text("Amount to Withdraw (₱)", fontSize = 12.sp, color = colors.textMuted)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value         = withdrawInput,
                        onValueChange = { withdrawInput = it },
                        placeholder   = { Text("0.00", color = colors.textMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors        = outlinedTextFieldColors(colors),
                        shape         = RoundedCornerShape(8.dp),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val amt = withdrawInput.toDoubleOrNull() ?: return@Button
                            if (amt <= 0) return@Button
                            if (amt > state.currentSavings) {
                                onToast("Not enough savings to withdraw that amount.", colors.danger)
                                return@Button
                            }
                            confirm("Withdraw ₱${"%.2f".format(amt)} from savings? This will deduct XP.") {
                                val leveledDown = viewModel.withdrawSavings(amt)
                                withdrawInput = ""
                                if (leveledDown) {
                                    onToast("⬇ Level Down! Back to ${getTitle(state.level)}", colors.danger)
                                } else {
                                    onToast("📤 Withdrawal recorded. XP deducted.", colors.border)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = colors.danger),
                        shape    = RoundedCornerShape(8.dp)
                    ) { Text("Withdraw  −1 XP", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }

        // ── Savings Goals Section ──────────────────────────────────────────────
        item {
            Text("Savings Goals", fontWeight = FontWeight.Bold,
                fontSize = 15.sp, color = colors.textPrimary)
        }

        // Add goal form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = colors.card),
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(1.dp, colors.accent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Goal Name", fontSize = 12.sp, color = colors.textMuted)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value         = goalNameInput,
                                onValueChange = { goalNameInput = it },
                                placeholder   = { Text("e.g. New Phone", color = colors.textMuted) },
                                colors        = outlinedTextFieldColors(colors),
                                shape         = RoundedCornerShape(8.dp),
                                singleLine    = true,
                                modifier      = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Target (₱)", fontSize = 12.sp, color = colors.textMuted)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value         = goalTargetInput,
                                onValueChange = { goalTargetInput = it },
                                placeholder   = { Text("0.00", color = colors.textMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors        = outlinedTextFieldColors(colors),
                                shape         = RoundedCornerShape(8.dp),
                                singleLine    = true,
                                modifier      = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val name   = goalNameInput.trim()
                            val target = goalTargetInput.toDoubleOrNull()
                            if (name.isEmpty() || target == null || target <= 0) return@Button
                            viewModel.addSavingsGoal(name, target)
                            goalNameInput   = ""
                            goalTargetInput = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape    = RoundedCornerShape(8.dp)
                    ) { Text("Add Goal", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }

        // Goal cards
        if (state.savingsGoals.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center) {
                    Text("No savings goals yet. Add one above!",
                        color = colors.textMuted, fontSize = 14.sp)
                }
            }
        }

        items(state.savingsGoals) { goal ->
            SavingsGoalCard(
                goal     = goal,
                saved    = state.currentSavings,
                colors   = colors,
                onClaim  = {
                    confirm("Claim \"${goal.name}\"? This will deduct ₱${"%.2f".format(goal.targetAmount)} from your savings.") {
                        viewModel.claimSavingsGoal(goal.id)
                    }
                },
                onRemove = {
                    confirm("Remove \"${goal.name}\"?") {
                        viewModel.removeSavingsGoal(goal.id)
                    }
                }
            )
        }
    }
}

// ── Savings Goal Card ─────────────────────────────────────────────────────────

@Composable
fun SavingsGoalCard(
    goal:     SavingsGoal,
    saved:    Double,
    colors:   com.mystegui.budgettracker.ui.theme.AppColors,
    onClaim:  () -> Unit,
    onRemove: () -> Unit
) {
    val target     = goal.targetAmount
    val pct        = if (target > 0) (saved / target).coerceIn(0.0, 1.0).toFloat() else 0f
    val canAfford  = saved >= target
    val achieved   = goal.achieved

    val borderColor = when {
        achieved  -> colors.success
        canAfford -> colors.warning
        else      -> colors.border
    }
    val barColor = when {
        achieved  -> colors.success
        canAfford -> colors.warning
        else      -> colors.accent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = colors.card),
        shape    = RoundedCornerShape(10.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Name + target row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    goal.name,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = if (achieved) colors.success else colors.textPrimary
                )
                Text(
                    "Target: ₱${"%.2f".format(target)}",
                    fontSize = 12.sp,
                    color    = colors.textMuted
                )
            }
            Spacer(Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress   = { if (achieved) 1f else pct },
                modifier   = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color      = barColor,
                trackColor = colors.border
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (achieved) "✅ Goal Achieved!"
                else "${"%.0f".format(pct * 100)}%  —  ₱${"%.2f".format(minOf(saved, target))} saved of ₱${"%.2f".format(target)}",
                fontSize = 11.sp,
                color    = if (achieved) colors.success else colors.textMuted
            )

            Spacer(Modifier.height(10.dp))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!achieved && canAfford) {
                    Button(
                        onClick  = onClaim,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = colors.success),
                        shape    = RoundedCornerShape(8.dp)
                    ) { Text("✔ Claim", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }
                if (achieved) {
                    Text(
                        "✅ Achieved",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp,
                        color      = colors.success,
                        modifier   = Modifier.weight(1f).padding(vertical = 8.dp)
                    )
                }
                OutlinedButton(
                    onClick  = onRemove,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(8.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, colors.danger)
                ) { Text("Remove", color = colors.danger, fontSize = 13.sp) }
            }
        }
    }
}