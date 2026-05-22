import javax.swing.*;

/**
 * Main — application entry point.
 *
 * Responsibilities:
 *   1. Apply system L&F as a base (ignored silently if unavailable)
 *   2. Apply the app's custom dark theme overrides
 *   3. Load persisted data from disk (theme is restored here too)
 *   4. Launch the splash screen, then hand off to BudgetApp
 */
public class Main {

    public static void main(String[] args) {
        // ── 1. Base L&F ───────────────────────────────────────────────────────
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // ── 2. Custom theme ───────────────────────────────────────────────────
        UITheme.applyDarkLookAndFeel();

        // ── 3. Load save data (theme is restored inside SaveManager.load) ─────
        SaveManager.load();

        // ── 4. Splash → main window ───────────────────────────────────────────
        SwingUtilities.invokeLater(() ->
                SplashScreen.show(() -> {
                    BudgetApp app = new BudgetApp();
                    app.setVisible(true);
                })
        );
    }
}