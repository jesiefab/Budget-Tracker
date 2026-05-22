package com.mystegui.budgettracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mystegui.budgettracker.AppState
import com.mystegui.budgettracker.BudgetViewModel
import com.mystegui.budgettracker.CATEGORIES
import com.mystegui.budgettracker.CATEGORY_COLORS
import com.mystegui.budgettracker.ui.theme.LocalAppColors

@Composable
fun HistoryScreen(state: AppState, viewModel: BudgetViewModel) {
    val colors = LocalAppColors.current
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = colors.card,
            title = {
                Text("Clear History", fontWeight = FontWeight.Bold,
                    color = colors.textPrimary)
            },
            text = {
                Text("Clear ALL expense history? This can't be undone.",
                    color = colors.textMuted, fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminResetExpenses()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text("Clear", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = colors.textMuted)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Header ─────────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Expense History", fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, color = colors.textPrimary)
                TextButton(
                    onClick = { showClearConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.danger)
                ) {
                    Text("Clear All", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Category Breakdown ─────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Category Breakdown", fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = colors.textPrimary)
                    Spacer(Modifier.height(12.dp))

                    val total = state.totalSpent

                    if (total == 0.0) {
                        Text("No expenses yet.", color = colors.textMuted,
                            fontSize = 14.sp)
                    } else {
                        CATEGORIES.forEach { cat ->
                            val catTotal = state.expenses
                                .filter { it.category == cat }
                                .sumOf { it.amount }
                            if (catTotal == 0.0) return@forEach

                            val pct = (catTotal / total).toFloat()
                            val catColor = Color(CATEGORY_COLORS[cat] ?: 0xFF6C757D)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(catColor)
                                    )
                                    Text(cat, fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary)
                                }
                                Text(
                                    "₱${"%.2f".format(catTotal)}  (${"%.0f".format(pct * 100)}%)",
                                    fontSize = 12.sp, color = colors.textMuted
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = catColor,
                                trackColor = colors.border
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        HorizontalDivider(color = colors.border, thickness = 1.dp)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grand Total", fontWeight = FontWeight.Bold,
                                fontSize = 14.sp, color = colors.textPrimary)
                            Text("₱${"%.2f".format(total)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp, color = colors.accent)
                        }
                    }
                }
            }
        }

        // ── Full Expense List ──────────────────────────────────────────────────
        item {
            Text("All Expenses", fontWeight = FontWeight.Bold,
                fontSize = 15.sp, color = colors.textPrimary)
        }

        if (state.expenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No expenses logged yet.",
                        color = colors.textMuted, fontSize = 14.sp)
                }
            }
        }

        items(state.expenses.reversed()) { expense ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(expense.description,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp, color = colors.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val catColor = Color(CATEGORY_COLORS[expense.category] ?: 0xFF6C757D)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(catColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(expense.category, color = catColor,
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(expense.date, fontSize = 11.sp,
                                color = colors.textMuted)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "₱${"%.2f".format(expense.amount)}",
                            fontWeight = FontWeight.Bold,
                            color = colors.danger, fontSize = 15.sp
                        )
                        IconButton(
                            onClick = { viewModel.removeExpense(expense.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}