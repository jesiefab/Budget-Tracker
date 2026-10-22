package com.mystegui.budgettracker;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

/**
 * Scaffold-stage launcher: proves the Spring→JavaFX handshake works end to end
 * (start() resolves this bean from the context and hands it the primary Stage).
 * Replaced by the real splash + FXML stage assembly during T12 — no logic worth
 * keeping should ever hang its hat on this placeholder.
 */
@Component
public class UiLauncher {

    public void show(Stage stage) {
        StackPane root = new StackPane(new Label("Budget Tracker v4 — scaffold (Spring booted, JavaFX running)"));
        stage.setScene(new Scene(root, 900, 620));
        stage.setTitle("Student Budget Tracker");
        stage.show();
    }
}
