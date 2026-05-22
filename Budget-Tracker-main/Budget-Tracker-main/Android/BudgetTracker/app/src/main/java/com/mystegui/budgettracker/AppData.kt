package com.mystegui.budgettracker

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

// ── XP / Ranking ──────────────────────────────────────────────────────────────

val TITLES = listOf(
    "", "Broke Boy", "Budget Apprentice", "Penny Pincher",
    "Frugal Warrior", "Savings Knight", "Money Mage",
    "Budget Royalty", "Wealth Sage", "Diamond Saver", "Financial Legend"
)

fun xpAtLevelStart(level: Int): Int = (1 until level).sumOf { it * 100 }

fun computeLevel(totalXP: Int): Int {
    var level = 1
    while (totalXP - xpAtLevelStart(level) >= level * 100) level++
    return level
}

fun xpForNext(level: Int)              = level * 100
fun currentLevelXP(totalXP: Int, level: Int) = totalXP - xpAtLevelStart(level)
fun calcXP(amount: Double)             = maxOf(5, (amount / 10).toInt())
fun getTitle(level: Int)               = TITLES[minOf(level, TITLES.size - 1)]

// ── Budget Period ─────────────────────────────────────────────────────────────

enum class BudgetPeriod(val displayName: String, val days: Int) {
    WEEKLY ("Weekly",  7),
    MONTHLY("Monthly", 30),
    YEARLY ("Yearly",  365);

    fun daysElapsed(): Int {
        val now = LocalDate.now()
        return when (this) {
            WEEKLY  -> now.dayOfWeek.value
            YEARLY  -> now.dayOfYear
            MONTHLY -> now.dayOfMonth
        }
    }

    fun periodStart(): LocalDate {
        val now = LocalDate.now()
        return when (this) {
            WEEKLY  -> now.minusDays((now.dayOfWeek.value - 1).toLong())
            YEARLY  -> LocalDate.of(now.year, 1, 1)
            MONTHLY -> LocalDate.of(now.year, now.month, 1)
        }
    }

    companion object {
        fun fromString(s: String) =
            entries.firstOrNull { it.displayName == s } ?: MONTHLY
    }
}

// ── Tracking mode ─────────────────────────────────────────────────────────────

enum class TrackingMode(val displayName: String) {
    BASIC("Basic"),
    ADVANCED("Advanced");

    companion object {
        fun fromString(s: String) =
            entries.firstOrNull { it.displayName == s } ?: BASIC
    }
}

// ── Savings Goal ──────────────────────────────────────────────────────────────

data class SavingsGoal(
    val id:           Long    = System.currentTimeMillis(),
    val name:         String,
    val targetAmount: Double,
    val achieved:     Boolean = false
)

// ── Budget math helpers ───────────────────────────────────────────────────────

fun proratedBudget(budget: Double, period: BudgetPeriod): Double {
    if (budget <= 0) return 0.0
    val elapsed = maxOf(1, period.daysElapsed())
    return (budget / period.days) * elapsed
}

fun dailyAllowance(remaining: Double, period: BudgetPeriod): Double {
    val daysLeft = period.days - period.daysElapsed()
    if (daysLeft <= 0) return 0.0
    return maxOf(remaining / daysLeft, 0.0)
}

// ── App State ─────────────────────────────────────────────────────────────────

data class AppState(
    val expenses:      List<Expense>      = emptyList(),
    val budget:        Double             = 0.0,
    val budgetPeriod:  BudgetPeriod       = BudgetPeriod.MONTHLY,
    val needsPercent:  Double             = 50.0,
    val trackingMode:  TrackingMode       = TrackingMode.BASIC,
    val savingsGoal:   Double             = 1000.0,
    val currentSavings: Double            = 0.0,
    val savingsGoals:  List<SavingsGoal>  = emptyList(),
    val totalXP:       Int                = 0,
    val level:         Int                = 1,
    val theme:         String             = "Dark"
) {
    // ── Period-filtered expenses ───────────────────────────────────────────────
    val periodStart get() = budgetPeriod.periodStart()

    val periodExpenses get() = expenses.filter {
        LocalDate.parse(it.date) >= periodStart
    }

    val totalSpentThisPeriod get() = periodExpenses.sumOf { it.amount }

    val needsSpentThisPeriod get() = periodExpenses
        .filter { NEEDS_CATEGORIES.contains(it.category) }
        .sumOf { it.amount }

    val wantsSpentThisPeriod get() = periodExpenses
        .filter { !NEEDS_CATEGORIES.contains(it.category) }
        .sumOf { it.amount }

    // ── All-time totals (for History) ─────────────────────────────────────────
    val totalSpent get() = expenses.sumOf { it.amount }

    // ── Budget pools ──────────────────────────────────────────────────────────
    val wantsPercent  get() = 100.0 - needsPercent
    val needsBudget   get() = budget * (needsPercent / 100.0)
    val wantsBudget   get() = budget * (wantsPercent / 100.0)

    // ── Total prorated ────────────────────────────────────────────────────────
    val prorated      get() = proratedBudget(budget, budgetPeriod)
    val remaining     get() = maxOf(prorated - totalSpentThisPeriod, 0.0)
    val spentPct      get() = if (prorated > 0) (totalSpentThisPeriod / prorated * 100).toFloat() else 0f
    val totalDailyAllowance get() = dailyAllowance(budget - totalSpentThisPeriod, budgetPeriod)

    // ── Needs prorated ────────────────────────────────────────────────────────
    val needsProrated get() = proratedBudget(needsBudget, budgetPeriod)
    val needsRemaining get() = maxOf(needsProrated - needsSpentThisPeriod, 0.0)
    val needsSpentPct get() = if (needsProrated > 0) (needsSpentThisPeriod / needsProrated * 100).toFloat() else 0f
    val needsDailyAllowance get() = dailyAllowance(needsBudget - needsSpentThisPeriod, budgetPeriod)

    // ── Wants prorated ────────────────────────────────────────────────────────
    val wantsProrated get() = proratedBudget(wantsBudget, budgetPeriod)
    val wantsRemaining get() = maxOf(wantsProrated - wantsSpentThisPeriod, 0.0)
    val wantsSpentPct get() = if (wantsProrated > 0) (wantsSpentThisPeriod / wantsProrated * 100).toFloat() else 0f
    val wantsDailyAllowance get() = dailyAllowance(wantsBudget - wantsSpentThisPeriod, budgetPeriod)

    // ── Savings ───────────────────────────────────────────────────────────────
    val savingsNeeded get() = maxOf(savingsGoal - currentSavings, 0.0)
    val savingsPct    get() = if (savingsGoal > 0) (currentSavings / savingsGoal * 100).toFloat() else 0f

    // ── XP ────────────────────────────────────────────────────────────────────
    val lvlXP  get() = currentLevelXP(totalXP, level)
    val nextXP get() = xpForNext(level)
    val xpPct  get() = if (nextXP > 0) (lvlXP.toFloat() / nextXP * 100) else 0f

    // ── Misc ──────────────────────────────────────────────────────────────────
    val dailyRate get() = if (budgetPeriod.days > 0) budget / budgetPeriod.days else 0.0
}

// Fixed needs category mapping
val NEEDS_CATEGORIES = setOf("Food", "Transport", "School", "Health")

// ── SaveManager ───────────────────────────────────────────────────────────────

class SaveManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("budget_tracker", Context.MODE_PRIVATE)

    fun save(state: AppState) {
        // Expenses
        val expArr = JSONArray()
        state.expenses.forEach { e ->
            expArr.put(JSONObject().apply {
                put("id",       e.id)
                put("desc",     e.description)
                put("amount",   e.amount)
                put("category", e.category)
                put("date",     e.date)
            })
        }

        // Savings goals
        val goalArr = JSONArray()
        state.savingsGoals.forEach { g ->
            goalArr.put(JSONObject().apply {
                put("id",       g.id)
                put("name",     g.name)
                put("target",   g.targetAmount)
                put("achieved", g.achieved)
            })
        }

        prefs.edit()
            .putString("expenses",       expArr.toString())
            .putFloat("budget",          state.budget.toFloat())
            .putString("budgetPeriod",   state.budgetPeriod.displayName)
            .putFloat("needsPercent",    state.needsPercent.toFloat())
            .putString("trackingMode",   state.trackingMode.displayName)
            .putFloat("savingsGoal",     state.savingsGoal.toFloat())
            .putFloat("currentSavings",  state.currentSavings.toFloat())
            .putString("savingsGoals",   goalArr.toString())
            .putInt("totalXP",           state.totalXP)
            .putInt("level",             state.level)
            .putString("theme",          state.theme)
            .apply()
    }

    fun load(): AppState {
        // Expenses
        val expArr = JSONArray(prefs.getString("expenses", "[]") ?: "[]")
        val expenses = (0 until expArr.length()).map { i ->
            expArr.getJSONObject(i).let { o ->
                Expense(
                    id          = o.getLong("id"),
                    description = o.getString("desc"),
                    amount      = o.getDouble("amount"),
                    category    = o.getString("category"),
                    date        = o.getString("date")
                )
            }
        }

        // Savings goals
        val goalArr = JSONArray(prefs.getString("savingsGoals", "[]") ?: "[]")
        val savingsGoals = (0 until goalArr.length()).map { i ->
            goalArr.getJSONObject(i).let { o ->
                SavingsGoal(
                    id           = o.getLong("id"),
                    name         = o.getString("name"),
                    targetAmount = o.getDouble("target"),
                    achieved     = o.getBoolean("achieved")
                )
            }
        }

        return AppState(
            expenses       = expenses,
            budget         = prefs.getFloat("budget", 0f).toDouble(),
            budgetPeriod   = BudgetPeriod.fromString(
                prefs.getString("budgetPeriod", "Monthly") ?: "Monthly"
            ),
            needsPercent   = prefs.getFloat("needsPercent", 50f).toDouble(),
            trackingMode   = TrackingMode.fromString(
                prefs.getString("trackingMode", "Basic") ?: "Basic"
            ),
            savingsGoal    = prefs.getFloat("savingsGoal", 1000f).toDouble(),
            currentSavings = prefs.getFloat("currentSavings", 0f).toDouble(),
            savingsGoals   = savingsGoals,
            totalXP        = prefs.getInt("totalXP", 0),
            level          = prefs.getInt("level", 1),
            theme          = prefs.getString("theme", "Dark") ?: "Dark"
        )
    }
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class BudgetViewModel(private val saveManager: SaveManager) : ViewModel() {
    private val _state = MutableStateFlow(saveManager.load())
    val state: StateFlow<AppState> = _state

    private fun update(block: AppState.() -> AppState) {
        _state.value = _state.value.block()
        saveManager.save(_state.value)
    }

    // ── Expenses ──────────────────────────────────────────────────────────────

    fun addExpense(desc: String, amount: Double, category: String) {
        val expense = Expense(
            description = desc,
            amount      = amount,
            category    = category,
            date        = LocalDate.now().toString()
        )
        update { copy(expenses = expenses + expense) }
    }

    fun removeExpense(id: Long) =
        update { copy(expenses = expenses.filter { it.id != id }) }

    // ── Budget ────────────────────────────────────────────────────────────────

    fun setBudget(amount: Double, period: BudgetPeriod) =
        update { copy(budget = amount, budgetPeriod = period) }

    fun setNeedsPercent(percent: Double) =
        update { copy(needsPercent = percent.coerceIn(0.0, 100.0)) }

    fun setTrackingMode(mode: TrackingMode) =
        update { copy(trackingMode = mode) }

    // ── Savings ───────────────────────────────────────────────────────────────

    fun setSavingsGoal(goal: Double) =
        update { copy(savingsGoal = goal) }

    fun addSavings(amount: Double): Int {
        val xpEarned   = calcXP(amount)
        val newTotalXP = _state.value.totalXP + xpEarned
        val newLevel   = computeLevel(newTotalXP)
        update { copy(currentSavings = currentSavings + amount, totalXP = newTotalXP, level = newLevel) }
        return xpEarned
    }

    /**
     * Withdraws from savings — deducts 1 XP per peso, minimum 1 XP.
     * Savings floor is 0. Returns true if level dropped.
     */
    fun withdrawSavings(amount: Double): Boolean {
        val current  = _state.value.currentSavings
        val actual   = minOf(amount, current)
        val penalty  = maxOf(1, actual.toInt())
        val newXP    = maxOf(0, _state.value.totalXP - penalty)
        val newLevel = computeLevel(newXP)
        val leveledDown = newLevel < _state.value.level
        update { copy(currentSavings = current - actual, totalXP = newXP, level = newLevel) }
        return leveledDown
    }

    // ── Multi-goal savings ────────────────────────────────────────────────────

    fun addSavingsGoal(name: String, target: Double) =
        update { copy(savingsGoals = savingsGoals + SavingsGoal(name = name, targetAmount = target)) }

    fun removeSavingsGoal(id: Long) =
        update { copy(savingsGoals = savingsGoals.filter { it.id != id }) }

    /**
     * Claims a savings goal — deducts target from currentSavings and marks achieved.
     * Returns false if already claimed or savings are insufficient.
     */
    fun claimSavingsGoal(id: Long): Boolean {
        val goal = _state.value.savingsGoals.find { it.id == id } ?: return false
        if (goal.achieved) return false
        if (_state.value.currentSavings < goal.targetAmount) return false
        update {
            copy(
                currentSavings = currentSavings - goal.targetAmount,
                savingsGoals   = savingsGoals.map {
                    if (it.id == id) it.copy(achieved = true) else it
                }
            )
        }
        return true
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    fun setTheme(theme: String) =
        update { copy(theme = theme) }

    // ── Admin resets ──────────────────────────────────────────────────────────

    fun adminResetSavings() =
        update { copy(currentSavings = 0.0, savingsGoal = 1000.0, savingsGoals = emptyList()) }

    fun adminResetXP() =
        update { copy(totalXP = 0, level = 1) }

    fun adminResetExpenses() =
        update { copy(expenses = emptyList()) }

    fun adminResetBudget() =
        update { copy(budget = 0.0, budgetPeriod = BudgetPeriod.MONTHLY, needsPercent = 50.0, trackingMode = TrackingMode.BASIC) }

    fun adminFullReset() =
        update {
            copy(
                expenses       = emptyList(),
                budget         = 0.0,
                budgetPeriod   = BudgetPeriod.MONTHLY,
                needsPercent   = 50.0,
                trackingMode   = TrackingMode.BASIC,
                savingsGoal    = 1000.0,
                currentSavings = 0.0,
                savingsGoals   = emptyList(),
                totalXP        = 0,
                level          = 1
            )
        }
}

// ── ViewModel Factory ─────────────────────────────────────────────────────────

class BudgetViewModelFactory(private val saveManager: SaveManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BudgetViewModel(saveManager) as T
    }
}