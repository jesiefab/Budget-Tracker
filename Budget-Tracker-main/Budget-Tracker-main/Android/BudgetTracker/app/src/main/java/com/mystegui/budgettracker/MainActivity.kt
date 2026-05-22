package com.mystegui.budgettracker

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mystegui.budgettracker.ui.BudgetScreen
import com.mystegui.budgettracker.ui.ExpenseScreen
import com.mystegui.budgettracker.ui.SavingsScreen
import com.mystegui.budgettracker.ui.HistoryScreen
import com.mystegui.budgettracker.ui.SettingsScreen
import com.mystegui.budgettracker.ui.theme.BudgetTrackerTheme
import com.mystegui.budgettracker.ui.theme.LocalAppColors
import com.mystegui.budgettracker.ui.theme.appColorsForTheme
import kotlinx.coroutines.delay

// ── Nav destinations ──────────────────────────────────────────────────────────

sealed class Screen(val label: String, val icon: String) {
    object Expenses : Screen("Expenses", "💸")
    object Budget   : Screen("Budget",   "💰")
    object Savings  : Screen("Savings",  "🎯")
    object History  : Screen("History",  "📊")
    object Settings : Screen("Settings", "⚙️")
}

val screens = listOf(
    Screen.Expenses,
    Screen.Budget,
    Screen.Savings,
    Screen.History,
    Screen.Settings
)

// ── Activity ──────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    private val viewModel: BudgetViewModel by viewModels {
        BudgetViewModelFactory(SaveManager(this))
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            ReminderManager.scheduleReminder(
                this,
                ReminderManager.getSavedHour(this),
                ReminderManager.getSavedMinute(this)
            )
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Already granted, schedule it
                    if (ReminderManager.isReminderEnabled(this)) {
                        ReminderManager.scheduleReminder(
                            this,
                            ReminderManager.getSavedHour(this),
                            ReminderManager.getSavedMinute(this)
                        )
                    }
                }
                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ReminderManager.createNotificationChannel(this)
        requestNotificationPermission()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val appColors = appColorsForTheme(state.theme)

            BudgetTrackerTheme(appColors = appColors) {
                BudgetApp(viewModel = viewModel)
            }
        }
    }
}

// ── Root Composable ───────────────────────────────────────────────────────────

@Composable
fun BudgetApp(viewModel: BudgetViewModel) {
    val colors = LocalAppColors.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Expenses) }
    var toastMessage by remember { mutableStateOf("") }
    var toastColor by remember { mutableStateOf(Color.Gray) }
    var showToast by remember { mutableStateOf(false) }

    fun triggerToast(msg: String, color: Color) {
        toastMessage = msg
        toastColor = color
        showToast = true
    }

    LaunchedEffect(showToast) {
        if (showToast) {
            delay(2500)
            showToast = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Scaffold(
            containerColor = colors.bg,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.card)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Text(
                        "Student Budget Tracker",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colors.accent
                    )
                    Text(
                        "Track it. Save it. Don't blow it.",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 12.dp),
                        color = colors.border,
                        thickness = 1.dp
                    )
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.card)
                ) {
                    HorizontalDivider(color = colors.border, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        screens.forEach { screen ->
                            val selected = currentScreen == screen
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentScreen = screen },
                                icon = {
                                    Text(screen.icon, fontSize = 22.sp)
                                },
                                label = {
                                    Text(
                                        screen.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold
                                        else FontWeight.Normal,
                                        color = if (selected) colors.accent
                                        else colors.textMuted
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor   = colors.accent,
                                    unselectedIconColor = colors.textMuted,
                                    indicatorColor      = colors.accent.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (currentScreen) {
                    Screen.Expenses -> ExpenseScreen(
                        state     = state,
                        viewModel = viewModel
                    )
                    Screen.Budget   -> BudgetScreen(
                        state     = state,
                        viewModel = viewModel
                    )
                    Screen.Savings  -> SavingsScreen(
                        state     = state,
                        viewModel = viewModel,
                        onToast   = { msg, color -> triggerToast(msg, color) }
                    )
                    Screen.History  -> HistoryScreen(
                        state     = state,
                        viewModel = viewModel
                    )
                    Screen.Settings -> SettingsScreen(
                        state     = state,
                        viewModel = viewModel
                    )
                }
            }
        }

        // Toast overlay
        AnimatedVisibility(
            visible  = showToast,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(toastColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    toastMessage,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}