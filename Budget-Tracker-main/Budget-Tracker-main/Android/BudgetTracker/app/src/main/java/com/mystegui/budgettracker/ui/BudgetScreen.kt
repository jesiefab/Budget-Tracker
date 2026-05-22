package com.mystegui.budgettracker.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.mystegui.budgettracker.BudgetPeriod
import com.mystegui.budgettracker.BudgetViewModel
import com.mystegui.budgettracker.TrackingMode
import com.mystegui.budgettracker.ui.theme.AppColors
import com.mystegui.budgettracker.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(state: AppState, viewModel: BudgetViewModel) {
    val colors = LocalAppColors.current

    var budgetInput      by remember { mutableStateOf("") }
    var needsPctInput    by remember { mutableStateOf("") }
    var selectedPeriod   by remember { mutableStateOf(state.budgetPeriod) }
    var periodExpanded   by remember { mutableStateOf(false) }

    val isAdvanced = state.trackingMode == TrackingMode.ADVANCED

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Settings Card ──────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = colors.card),
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(1.dp, colors.accent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Budget Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = colors.accent
                    )
                    Spacer(Modifier.height(12.dp))

                    // Row 1: period + amount
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Period", fontSize = 12.sp, color = colors.textMuted)
                            Spacer(Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded        = periodExpanded,
                                onExpandedChange = { periodExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value         = selectedPeriod.displayName,
                                    onValueChange = {},
                                    readOnly      = true,
                                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodExpanded) },
                                    colors        = outlinedTextFieldColors(colors),
                                    shape         = RoundedCornerShape(8.dp),
                                    singleLine    = true,
                                    modifier      = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded        = periodExpanded,
                                    onDismissRequest = { periodExpanded = false },
                                    modifier        = Modifier.background(colors.card)
                                ) {
                                    BudgetPeriod.entries.forEach { period ->
                                        DropdownMenuItem(
                                            text    = { Text(period.displayName, color = colors.textPrimary) },
                                            onClick = { selectedPeriod = period; periodExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Amount (₱)", fontSize = 12.sp, color = colors.textMuted)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value         = budgetInput,
                                onValueChange = { budgetInput = it },
                                placeholder   = { Text("${"%.2f".format(state.budget)}", color = colors.textMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors        = outlinedTextFieldColors(colors),
                                shape         = RoundedCornerShape(8.dp),
                                singleLine    = true,
                                modifier      = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Row 2: needs % (advanced only)
                    if (isAdvanced) {
                        Spacer(Modifier.height(10.dp))
                        Text("Needs % (rest goes to Wants)", fontSize = 12.sp, color = colors.textMuted)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value         = needsPctInput,
                            onValueChange = { needsPctInput = it },
                            placeholder   = { Text("${"%.0f".format(state.needsPercent)}", color = colors.textMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors        = outlinedTextFieldColors(colors),
                            shape         = RoundedCornerShape(8.dp),
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Period info hint
                    val elapsed = selectedPeriod.daysElapsed()
                    Text(
                        "Day $elapsed of ${selectedPeriod.days}  •  Daily rate: ₱${"%.2f".format(state.dailyRate)}",
                        fontSize = 12.sp,
                        color    = colors.textMuted
                    )

                    Spacer(Modifier.height(10.dp))

                    // Set Budget button
                    Button(
                        onClick = {
                            val amt = budgetInput.toDoubleOrNull() ?: return@Button
                            if (amt <= 0) return@Button
                            viewModel.setBudget(amt, selectedPeriod)
                            if (isAdvanced) {
                                val pct = needsPctInput.toDoubleOrNull()
                                if (pct != null && pct in 0.0..100.0) {
                                    viewModel.setNeedsPercent(pct)
                                }
                            }
                            budgetInput   = ""
                            needsPctInput = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape    = RoundedCornerShape(8.dp)
                    ) {
                        Text("Set Budget", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(8.dp))

                    // Basic / Advanced toggle
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick  = { viewModel.setTrackingMode(TrackingMode.BASIC) },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = if (!isAdvanced) colors.accent else colors.border
                            ),
                            shape    = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Basic",
                                color      = if (!isAdvanced) Color.White else colors.textMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick  = { viewModel.setTrackingMode(TrackingMode.ADVANCED) },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = if (isAdvanced) colors.accent else colors.border
                            ),
                            shape    = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Advanced",
                                color      = if (isAdvanced) Color.White else colors.textMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ── Status Card ────────────────────────────────────────────────────────
        item {
            val pct = state.spentPct
            val barColor = when {
                pct >= 100f -> colors.danger
                pct >= 80f  -> colors.warning
                else        -> colors.success
            }
            val statusText = when {
                pct >= 100f -> "Budget exceeded for this period!"
                pct >= 80f  -> "Getting close to your prorated limit."
                else        -> "On track — ${"%.0f".format(pct)}% of prorated budget used"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = colors.card),
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "${state.budgetPeriod.displayName} Budget: ₱${"%.2f".format(state.budget)}" +
                                if (isAdvanced) "  •  Needs: ${"%.0f".format(state.needsPercent)}% / Wants: ${"%.0f".format(state.wantsPercent)}%" else "",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = colors.textPrimary
                    )
                    Spacer(Modifier.height(10.dp))

                    // Total progress bar
                    LinearProgressIndicator(
                        progress    = { (pct / 100f).coerceIn(0f, 1f) },
                        modifier    = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color       = barColor,
                        trackColor  = colors.border
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(statusText, fontSize = 12.sp, color = barColor)
                    Spacer(Modifier.height(16.dp))

                    if (!isAdvanced) {
                        // ── Basic stat cards ───────────────────────────────────
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard(
                                label       = "Should've Spent",
                                value       = "₱${"%.2f".format(state.prorated)}",
                                valueColor  = colors.accent,
                                cardColor   = colors.card,
                                borderColor = colors.border,
                                modifier    = Modifier.weight(1f)
                            )
                            StatCard(
                                label       = "Actually Spent",
                                value       = "₱${"%.2f".format(state.totalSpentThisPeriod)}",
                                valueColor  = colors.danger,
                                cardColor   = colors.card,
                                borderColor = colors.border,
                                modifier    = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard(
                                label       = "Today's Buffer",
                                value       = "₱${"%.2f".format(state.remaining)}",
                                valueColor  = colors.success,
                                cardColor   = colors.card,
                                borderColor = colors.border,
                                modifier    = Modifier.weight(1f)
                            )
                            StatCard(
                                label       = "Safe to Spend",
                                value       = "₱${"%.2f".format(state.totalDailyAllowance)}",
                                valueColor  = colors.warning,
                                cardColor   = colors.card,
                                borderColor = colors.border,
                                modifier    = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // ── Advanced: needs + wants pool cards ─────────────────
                        PoolCard(
                            label           = "NEEDS",
                            categoryHint    = "Food, Transport, School, Health",
                            budget          = state.needsBudget,
                            prorated        = state.needsProrated,
                            spent           = state.needsSpentThisPeriod,
                            remaining       = state.needsRemaining,
                            dailyAllowance  = state.needsDailyAllowance,
                            spentPct        = state.needsSpentPct,
                            borderColor     = colors.accent,
                            colors          = colors
                        )
                        Spacer(Modifier.height(10.dp))
                        PoolCard(
                            label           = "WANTS",
                            categoryHint    = "Entertainment, Shopping, Other",
                            budget          = state.wantsBudget,
                            prorated        = state.wantsProrated,
                            spent           = state.wantsSpentThisPeriod,
                            remaining       = state.wantsRemaining,
                            dailyAllowance  = state.wantsDailyAllowance,
                            spentPct        = state.wantsSpentPct,
                            borderColor     = colors.warning,
                            colors          = colors
                        )
                    }
                }
            }
        }
    }
}

// ── Pool card (needs / wants) ─────────────────────────────────────────────────

@Composable
fun PoolCard(
    label:          String,
    categoryHint:   String,
    budget:         Double,
    prorated:       Double,
    spent:          Double,
    remaining:      Double,
    dailyAllowance: Double,
    spentPct:       Float,
    borderColor:    Color,
    colors:         AppColors
) {
    val barColor = when {
        spentPct >= 100f -> colors.danger
        spentPct >= 80f  -> colors.warning
        else             -> colors.success
    }
    val statusText = when {
        spentPct >= 100f -> "Exceeded prorated $label budget."
        spentPct >= 80f  -> "Getting close to the $label limit."
        else             -> "On track with $label budget."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = colors.card),
        shape    = RoundedCornerShape(10.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = borderColor)
            Text(categoryHint, fontSize = 11.sp, color = colors.textMuted)
            Spacer(Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress   = { (spentPct / 100f).coerceIn(0f, 1f) },
                modifier   = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color      = barColor,
                trackColor = colors.border
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${"%.0f".format(spentPct)}% of prorated $label budget used  •  $statusText",
                fontSize = 11.sp,
                color    = barColor
            )
            Spacer(Modifier.height(10.dp))

            // 4 stat cards in a 2x2 grid
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    label       = "Should've Spent",
                    value       = "₱${"%.2f".format(prorated)}",
                    valueColor  = colors.accent,
                    cardColor   = colors.card,
                    borderColor = colors.border,
                    modifier    = Modifier.weight(1f)
                )
                StatCard(
                    label       = "Actually Spent",
                    value       = "₱${"%.2f".format(spent)}",
                    valueColor  = colors.danger,
                    cardColor   = colors.card,
                    borderColor = colors.border,
                    modifier    = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    label       = "Today's Buffer",
                    value       = "₱${"%.2f".format(remaining)}",
                    valueColor  = colors.success,
                    cardColor   = colors.card,
                    borderColor = colors.border,
                    modifier    = Modifier.weight(1f)
                )
                StatCard(
                    label       = "Safe to Spend",
                    value       = "₱${"%.2f".format(dailyAllowance)}",
                    valueColor  = colors.warning,
                    cardColor   = colors.card,
                    borderColor = colors.border,
                    modifier    = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    label:       String,
    value:       String,
    valueColor:  Color,
    cardColor:   Color,
    borderColor: Color,
    modifier:    Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = cardColor),
        shape    = RoundedCornerShape(10.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier            = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = valueColor)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 10.sp, color = borderColor)
        }
    }
}