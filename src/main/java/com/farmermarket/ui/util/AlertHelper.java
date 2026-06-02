package com.farmermarket.ui.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 Utility class for displaying JavaFX {dialogs.
 */
public final class AlertHelper {

    // Prevent instantiation
    private AlertHelper() {}

    public static void showError(String message) {
        showOnFxThread(Alert.AlertType.ERROR, "Error", message);
    }


    public static void showInfo(String message) {
        showOnFxThread(Alert.AlertType.INFORMATION, "Information", message);
    }


    public static boolean showConfirmation(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message,
                ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Confirm");
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }


    private static void showOnFxThread(Alert.AlertType type, String title, String message) {
        if (Platform.isFxApplicationThread()) {
            show(type, title, message);
        } else {
            Platform.runLater(() -> show(type, title, message));
        }
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
