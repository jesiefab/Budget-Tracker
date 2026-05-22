package com.mystegui.budgettracker

data class Expense(
    val id: Long = System.currentTimeMillis(),
    val description: String,
    val amount: Double,
    val category: String,
    val date: String
)

val CATEGORIES = listOf(
    "Food", "Transport", "School",
    "Entertainment", "Health", "Shopping", "Other"
)

val CATEGORY_COLORS = mapOf(
    "Food"          to 0xFFE07B39,
    "Transport"     to 0xFF4A90D9,
    "School"        to 0xFF5AA96B,
    "Entertainment" to 0xFF9B6DBF,
    "Health"        to 0xFFD9534F,
    "Shopping"      to 0xFFD4A017,
    "Other"         to 0xFF6C757D
)