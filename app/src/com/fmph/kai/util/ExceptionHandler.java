package com.fmph.kai.util;

import javafx.scene.control.Alert;

/**
 * Handles the exceptions raised during the runtime.
 */
public class ExceptionHandler {
    /**
     * Prints out the error message to the console,
     * TODO: logs the error into the log file,
     * creates an Alert GUI element delivering the error message.
     * @param e is the exception caught
     */
    public static void handle(Exception e) {
        System.out.println(e.getMessage());
        // TODO: Logger.log(e.getMessage());
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("An error has occurred!");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}
