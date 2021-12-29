package com.fmph.kai.util;

import javafx.scene.control.Alert;

public class ExceptionHandler {
    public static void handle(Exception e) {
        System.out.println(e.getMessage());
        // TODO: Logger.log(e.getMessage());
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("An error has occurred!");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}
