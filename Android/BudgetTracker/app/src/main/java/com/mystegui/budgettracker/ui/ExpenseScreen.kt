package com.mystegui.budgettracker.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.mystegui.budgettracker.CATEGORIES
import com.mystegui.budgettracker.CATEGORY_COLORS
import com.mystegui.budgettracker.AppState
import com.mystegui.budgettracker.BudgetViewModel
import com.mystegui.budgettracker.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(state: AppState, viewModel: BudgetViewModel) {
    val colors = LocalAppColors.current

    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val totalSpent = state.totalSpent

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Add Expense Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Expense", fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = colors.textPrimary)
                    Spacer(Modifier.height(12.dp))

                    if (error.isNotEmpty()) {
                        Text(error, color = colors.danger, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                    }

                    Text("Description", fontSize = 12.sp, color = colors.textMuted)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        placeholder = { Text("e.g. Lunch at canteen", color = colors.textMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedTextFieldColors(colors),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Amount (₱)", fontSize = 12.sp, color = colors.textMuted)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = amount,
                                onValueChange = { amount = it },
                                placeholder = { Text("0.00", color = colors.textMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = outlinedTextFieldColors(colors),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Category", fontSize = 12.sp, color = colors.textMuted)
                            Spacer(Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = categoryExpanded,
                                onExpandedChange = { categoryExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                    colors = outlinedTextFieldColors(colors),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = categoryExpanded,
                                    onDismissRequest = { categoryExpanded = false },
                                    modifier = Modifier.background(colors.card)
                                ) {
                                    CATEGORIES.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat, color = colors.textPrimary) },
                                            onClick = { category = cat; categoryExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (desc.isBlank()) { error = "Description required."; return@Button }
                            val amt = amount.toDoubleOrNull()
                            if (amt == null || amt <= 0) { error = "Enter a valid amount."; return@Button }
                            viewModel.addExpense(desc.trim(), amt, category)
                            desc = ""; amount = ""; error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add Expense", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            // Recent Expenses header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Expenses", fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, color = colors.textPrimary)
                Text("₱${"%.2f".format(totalSpent)}",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.accent)
            }
        }

        if (state.expenses.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center) {
                    Text("No expenses yet.", color = colors.textMuted, fontSize = 14.sp)
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
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(expense.description, fontWeight = FontWeight.SemiBold,
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
                            Text(expense.date, fontSize = 11.sp, color = colors.textMuted)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("₱${"%.2f".format(expense.amount)}",
                            fontWeight = FontWeight.Bold, color = colors.danger, fontSize = 15.sp)
                        IconButton(onClick = { viewModel.removeExpense(expense.id) },
                            modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove",
                                tint = colors.textMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun outlinedTextFieldColors(colors: com.mystegui.budgettracker.ui.theme.AppColors) =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor      = colors.textPrimary,
        unfocusedTextColor    = colors.textPrimary,
        focusedContainerColor = colors.input,
        unfocusedContainerColor = colors.input,
        focusedBorderColor    = colors.accent,
        unfocusedBorderColor  = colors.border,
        cursorColor           = colors.accent
    )