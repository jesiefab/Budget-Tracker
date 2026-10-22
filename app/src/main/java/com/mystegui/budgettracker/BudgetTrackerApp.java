package com.mystegui.budgettracker;

import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Bootstrap adapter — the only glue between Spring and JavaFX (design.md §2).
 *
 * Lifecycle ownership:
 *   - JavaFX owns the desktop lifecycle: main() asks Application.launch() to start,
 *     and the JavaFX launcher constructs this class itself (Spring does not).
 *   - Spring owns the application context: init() (launcher thread) boots it with
 *     WebApplicationType.NONE, start() (FX thread) shows the stage via UiLauncher,
 *     stop() closes the context so pending write work flushes before exit.
 *
 * Rules (do not relax):
 *   - init() stays free of Stage/Scene/JavaFX-UI work.
 *   - All scene-graph construction happens on the JavaFX Application Thread
 *     (here: inside start(), or later via Platform.runLater from services).
 */
@SpringBootApplication
public class BudgetTrackerApp extends Application {

    private ConfigurableApplicationContext context;

    public static void main(String[] args) {
        Application.launch(BudgetTrackerApp.class, args);
    }

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(String[]::new);
        context = new SpringApplicationBuilder(BudgetTrackerApp.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Override
    public void start(Stage stage) {
        context.getBean(UiLauncher.class).show(stage);
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }
}
