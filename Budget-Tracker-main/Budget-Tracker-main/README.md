# 💸 Student Budget Tracker

A budgeting app built for students to track expenses, manage budgets, and build saving habits — with a gamified XP and ranking system to keep you motivated.

Available on **Windows desktop** and **Android**. No internet connection required. All data saved locally on your device.

---

## 📥 Download

Head to the [Releases](../../releases) page and download the latest version for your platform.

| Platform | File | Requirements |
|---|---|---|
| Windows | `BudgetTracker.exe` | Java 11 or higher |
| Android | `BudgetTracker.apk` | Android 8.0 or higher |

> **Windows:** Download Java at [adoptium.net](https://adoptium.net) if you don't have it.  
> **Android:** Enable **Install from unknown sources** in Settings → Security before installing the APK.

---

## ✨ Features

### 💸 Expense Tracking
- Log expenses with a description, amount, category, and a **Need / Want** tag via a modal dialog overlay
- Tag each expense as a **Need** (green) or a **Want** (orange) using a two-button toggle — this affects Advanced budget calculations directly
- Search and filter your expense list by description (live text search) and by category
- The running total updates in real time to reflect only the filtered results
- Remove individual entries from the list with confirmation
- The **[Need]** or **[Want]** tag is shown inline next to each expense in the list

### 💰 Budget Management
- Set a **Weekly**, **Monthly**, or **Yearly** budget
- **Period-filtered tracking** — only expenses logged within the current budget period count toward your budget
- **Prorated tracking** — compares your spending against where you *should* be at this point in the period
- Live progress bar with color-coded warnings (green → yellow → red)
- Four stat cards: This Period Spent, Prorated Budget, Today's Buffer, Safe to Spend
- **Spending breakdown donut chart** — visualizes all-time spending by category with a color-coded legend; updates live as expenses are added
- **Basic mode** — single progress bar and four stat cards for the total budget
- **Advanced mode** — splits the budget into separate **Needs** and **Wants** pools based on each expense's Need / Want tag, each with their own progress bar and stat cards
- The Basic / Advanced toggle sits below the donut chart

### 🎯 Savings Tracker
- Set a savings goal and log deposits or withdrawals toward it
- Progress bar toward your goal with motivational messages
- **XP bar** displayed full-width at the top of the panel
- Info cards (Set Goal, Add/Withdraw, Savings Progress, Saver Rank) stack in a scrollable left column
- Add multiple named savings goals with individual targets and progress bars on the right column
- Claim a goal when your savings reach the target — deducts the amount from your savings pool
- Remove goals at any time

### ⚡ XP & Ranking System
- Earn XP every time you save money (+1 XP per ₱10, minimum +5 XP per deposit)
- Withdrawals deduct XP (and can trigger a level-down)
- Level up through 10 ranks:

| Level | Title |
|-------|-------|
| 1 | Broke Boy |
| 2 | Budget Apprentice |
| 3 | Penny Pincher |
| 4 | Frugal Warrior |
| 5 | Savings Knight |
| 6 | Money Mage |
| 7 | Budget Royalty |
| 8 | Wealth Sage |
| 9 | Diamond Saver |
| 10 | Financial Legend |

- Level-up animation and toast notification on rank up
- XP bar resets and scales harder each level

### 📊 Expense History
- Full table of all logged expenses with date, **Need / Want type**, category, description, and amount
- The Type column is color-coded — Need in accent blue, Want in orange
- Category breakdown panel with per-category progress bars and percentages
- Clear all history option

### 🎨 Themes
Five built-in color themes, applied instantly and saved between sessions:
- Dark *(default)*
- Light
- Midnight Blue
- Forest Green
- Warm Sunset

### ⚙️ Settings
- Accessible via the **gear icon** on the far right of the top navigation bar
- Theme switcher
- Password-protected Admin Tools panel (for resetting progress during testing)

### 💾 Persistence & Backup
- All data saves automatically on close
- Desktop: auto-saves when the app is closed, with an automatic `.bak` backup file created on every save
- Android: saves instantly to device storage on every change
- Save files from v2.0 are fully compatible — expenses without a Need / Want tag default to Need on load

### 🔔 Daily Reminder *(Android only)*
- Optional daily notification to remind you to log your expenses
- Customizable reminder time with a built-in time picker
- Defaults to 8:00 PM — persists through phone restarts

---

## 🖼️ Screenshots
<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/58c43fe4-3a3f-462f-a317-ea797f008a68" />
<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/cebc168d-fcca-4c24-8316-95d7f83783b3" />
<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/47f11360-412f-429b-948f-1357d06342b7" />
<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/d2930ea6-614f-40e7-a0cf-03ab76dbace2" />
<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/7903242d-c550-49c5-95c0-5aaf620a8fde" />
<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/799fd50a-1628-4181-86a6-5c6651854872" />
<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/8879ad22-d41b-486f-bf8d-0db278ebaf69" />
<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/6aeb3086-8d9f-4a79-8ab0-77a913b88e65" />
<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/7aa8111e-2c2d-4677-a47b-14e8bb0c9e75" />


---

## 🛠️ Building from Source

### Desktop (Windows)

**Requirements:** JDK 11 or higher

```bash
# Clone the repo
git clone https://github.com/YourUsername/StudentBudgetTracker.git
cd StudentBudgetTracker/desktop

# Compile
javac *.java

# Run
java Main

# Package as JAR
jar cfe BudgetTracker.jar Main *.class Savings_Tracker_Icon.png
```

To build the `.exe`, use [Launch4j](http://launch4j.sourceforge.net) pointing to the JAR with `Main` as the entry point.

### Android

**Requirements:** Android Studio, JDK 11 or higher

```bash
# Clone the repo
git clone https://github.com/YourUsername/StudentBudgetTracker.git
cd StudentBudgetTracker/android

# Open in Android Studio and run, or build APK via:
# Build → Build Bundle(s) / APK(s) → Build APK(s)
```

---

## 📁 Project Structure

```
StudentBudgetTracker/
├── desktop/                        # Windows Java Swing app
│   ├── Main.java                   # Entry point + splash screen hook
│   ├── BudgetApp.java              # Main JFrame, custom nav bar, CardLayout
│   ├── UITheme.java                # Colors, fonts, theme system
│   ├── DataStore.java              # Singleton data layer, XP logic
│   ├── SaveManager.java            # File persistence and backup
│   ├── SplashScreen.java           # Startup splash screen
│   ├── ExpensePanel.java           # Expenses tab — search/filter + add dialog
│   ├── BudgetPanel.java            # Budget tab — donut chart + stat cards
│   ├── SavingsPanel.java           # Savings + XP gamification tab
│   ├── HistoryPanel.java           # Expense history and breakdown
│   ├── SettingsPanel.java          # Theme switcher + admin tools
│   ├── Expense.java                # Expense data model (incl. needWant field)
│   └── Savings_Tracker_Icon.png
│
├── android/                        # Android Jetpack Compose app
│   ├── app/src/main/java/com/mystegui/budgettracker/
│   │   ├── MainActivity.kt         # Entry point + navigation
│   │   ├── AppData.kt              # State, ViewModel, SaveManager, XP logic
│   │   ├── Expense.kt              # Expense data model
│   │   ├── ReminderManager.kt      # Daily notification scheduler
│   │   ├── ReminderReceiver.kt     # Broadcast receiver for notifications
│   │   └── ui/
│   │       ├── ExpenseScreen.kt    # Expenses tab
│   │       ├── BudgetScreen.kt     # Budget tab
│   │       ├── SavingsScreen.kt    # Savings + XP tab
│   │       ├── HistoryScreen.kt    # Expense history tab
│   │       ├── SettingsScreen.kt   # Theme + admin + reminder settings
│   │       └── theme/
│   │           └── Theme.kt        # Color themes
│
└── README.md
```

---

## 📝 Notes

### Desktop
- Save data is stored as plain text in your home directory — you can open it in any text editor
- The `.bak` backup file is automatically created every time you save
- Save file location: `C:\Users\YourName\budget_tracker_save.dat`
- Save files from v2.0 load without issues — expenses missing a Need / Want tag default to Need

### Android
- Data is stored locally on your device — uninstalling the app will erase your data
- The APK is not from the Google Play Store so you'll need to allow unknown sources to install it
- For the daily reminder to persist after a phone restart, the app needs to have been opened at least once after boot

---

## 🔄 Changelog

### v3.0
- Added a custom **top navigation bar** — logo, app name, rounded nav buttons (Expenses, Budget, Savings, History), and a gear icon for Settings on the far right; Settings is no longer a tab
- Added **Need / Want tagging** for expenses — each expense is tagged at entry time via a two-button toggle; the tag is shown in the expense list, the History table, and is saved to disk
- **Add Expense** is now a modal overlay dialog instead of an inline form; the main panel has been replaced by a search and filter bar
- **Expense search and filter** — filter by description (live text) and by category; the total and count update to reflect filtered results only
- Added a **spending breakdown donut chart** to the Budget tab, showing all-time category spending with a legend; the Basic / Advanced toggle moved to below the chart
- **Advanced budget mode** now calculates Needs and Wants pools from each expense's actual Need / Want tag — the old hardcoded category mapping (Food = Need, Entertainment = Want, etc.) has been removed
- **Savings panel redesigned** — XP bar is now full-width at the top; info cards are in a scrollable left column; Add Goal form and goal list are in the right column
- Save file format updated from 4-field to 5-field pipe format for expenses; old saves are backward compatible

### v0.2
- Added **Spend Today** stat card — shows how much you can safely spend today based on remaining budget divided by days left in the period
- Renamed **Total Spent** stat card to **This Period Spent** to accurately reflect period-filtered tracking
- Budget calculations now only count expenses from the current budget period — expenses from previous periods no longer affect your current budget numbers
- Removed manual Save Now button — data saves automatically when the app is closed
- Code optimization pass across all source files: dead code removed, section labels added, logic bug fixes in theme system

### v0.1
- Initial release

---

## 👤 Author

Made by **MysteGUI**  
Built as a personal project for student budget management.

---

*Track it. Save it. Don't blow it.*
