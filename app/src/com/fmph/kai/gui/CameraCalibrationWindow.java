package com.fmph.kai.gui;

import com.fmph.kai.camera.Calibration;
import com.fmph.kai.camera.Capture;
import com.fmph.kai.util.ExceptionHandler;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.io.File;

/**
 * Handles the GUI of the calibration window.
 */
public class CameraCalibrationWindow extends Stage {
    private final Capture capture;
    private Calibration calibration;

    private int numRequired = 20;
    private int horSize = 9;
    private int verSize = 6;

    private final Button btnTakeSnapshot;

    /**
     * Creates the class instance (not showing the window).
     * Defines all GUI elements and their behaviour.
     * @param x is the left margin of the window
     * @param y is the top margin of the window
     * @param width is the width of the window
     * @param height is the height of the window
     * @throws Capture.CaptureException if failed to get the available cameras
     */
    public CameraCalibrationWindow(double x, double y, double width, double height) throws Capture.CaptureException {
        super();
        this.setTitle("Camera calibration");

        BorderPane pane = new BorderPane();
        calibration = new Calibration(new Size(horSize, verSize), numRequired);

        // TOP
        HBox top = new HBox(10);
        top.setPadding(new Insets(40, 40, 40, 40));
        top.setAlignment(Pos.CENTER);
        Label lblSelectCamera = new Label("Select camera: ");
        ComboBox<Integer> cmbSelectCamera = new ComboBox<>();
        cmbSelectCamera.setItems(Capture.getAvailableCameras());
        Label lblBoards = new Label("Samples #");
        TextField txtSnapshots = new TextField("20");
        txtSnapshots.setMaxWidth(50);
        Label lblHorizontalCorners = new Label("Horizontal corners #");
        TextField txtHorizontalCorners = new TextField("9");
        txtHorizontalCorners.setMaxWidth(50);
        Label lblVerticalCorners = new Label("Vertical corners #");
        TextField txtVerticalCorners = new TextField("6");
        txtVerticalCorners.setMaxWidth(50);
        Button btnUpdate = new Button("Update");
        top.getChildren().addAll(lblSelectCamera, cmbSelectCamera, lblBoards, txtSnapshots, lblHorizontalCorners, txtHorizontalCorners, lblVerticalCorners, txtVerticalCorners, btnUpdate);
        pane.setTop(top);

        // LEFT
        ImageView imgNormal = new ImageView();
        imgNormal.setFitWidth(width/2);
        imgNormal.setPreserveRatio(true);
        pane.setLeft(imgNormal);

        // RIGHT
        ImageView imgCalibrated = new ImageView();
        imgCalibrated.setFitWidth(width/2);
        imgCalibrated.setPreserveRatio(true);
        pane.setRight(imgCalibrated);

        // BOTTOM
        HBox bottom = new HBox(10);
        bottom.setPadding(new Insets(20, 20, 20, 20));
        bottom.setAlignment(Pos.CENTER);
        Button btnStartCamera = new Button("Start camera");
        btnTakeSnapshot = new Button("Take snapshot (" + 0 + "/" + numRequired + ")");
        btnTakeSnapshot.setDisable(true);
        Button btnSaveCalibration = new Button("Save calibration");
        btnSaveCalibration.setDisable(true);
        Calibration.OnCalibrated onCalibrated = () -> {
            btnTakeSnapshot.setDisable(true);
            btnSaveCalibration.setDisable(false);
        };
        calibration.setOnCalibrated(onCalibrated);
        bottom.getChildren().addAll(btnStartCamera, btnTakeSnapshot, btnSaveCalibration);
        pane.setBottom(bottom);

        // SCENE
        Scene scene = new Scene(pane, width, height);
        this.setScene(scene);

        this.setX(x);
        this.setY(y);

        // Top panel actions
        btnUpdate.setOnAction(e -> {
            horSize = Integer.parseInt(txtHorizontalCorners.getText());
            verSize = Integer.parseInt(txtVerticalCorners.getText());
            numRequired = Integer.parseInt(txtSnapshots.getText());
            calibration = new Calibration(new Size(horSize, verSize), numRequired);
            calibration.setOnCalibrated(onCalibrated);
            btnTakeSnapshot.setText("Take snapshot (" + 0 + "/" + numRequired + ")");
        });

        // VideoCapture
        capture = new Capture();
        capture.setOnFrameReceived(frame -> {
            Image calibratedImage = null;
            if (calibration.isCalibrated()) {
                calibratedImage = Capture.Mat2Image(calibration.calibrateImage(frame));
            }
            btnTakeSnapshot.setDisable(!calibration.findAndDrawPoints(frame));
            imgNormal.setImage(Capture.Mat2Image(frame));
            imgCalibrated.setImage(calibratedImage);
        });
        btnStartCamera.setOnAction(e -> {
            try {
                if (capture.isCapturing()) {
                    btnStartCamera.setText("Start camera");
                    capture.stop();
                    imgNormal.setImage(null);
                    imgCalibrated.setImage(null);
                } else {
                    capture.start(cmbSelectCamera.getValue());
                    btnStartCamera.setText("Stop camera");
                }
            } catch (Capture.CaptureException exception) {
                ExceptionHandler.handle(exception);
            }
        });

        // Calibration
        btnTakeSnapshot.setOnAction(e -> {
            calibration.newSnapshot();
            btnTakeSnapshot.setText("Take snapshot (" + calibration.getNumSnapshots() + "/" + numRequired + ")");
        });

        // Save the calibration
        btnSaveCalibration.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            directoryChooser.setTitle("Select the directory to save the calibration into");
            File dir = directoryChooser.showDialog(this);
            if (dir != null) {
                calibration.saveCalibration(dir.getAbsolutePath());
            }
        });

        this.setOnHiding(e -> {
            try {
                capture.stop();
            } catch (Capture.CaptureException ignored) {}
        });
    }
}
