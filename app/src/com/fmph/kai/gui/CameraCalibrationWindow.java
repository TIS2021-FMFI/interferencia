package com.fmph.kai.gui;

import com.fmph.kai.camera.Calibration;
import com.fmph.kai.camera.Capture;
import com.fmph.kai.util.ExceptionHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.opencv.core.Size;

public class CameraCalibrationWindow extends Stage {
    private final Capture capture;
    private Calibration calibration;

    private int numRequired = 20;
    private int horSize = 9;
    private int verSize = 6;

    private final Button btnTakeSnapshot;

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
        calibration.setOnCalibrated(() -> btnTakeSnapshot.setDisable(true));
        bottom.getChildren().addAll(btnStartCamera, btnTakeSnapshot);
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
            calibration.setOnCalibrated(() -> btnTakeSnapshot.setDisable(true));
            btnTakeSnapshot.setText("Take snapshot (" + 0 + "/" + numRequired + ")");
        });

        // VideoCapture
        capture = new Capture();
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
                    capture.setOnFrameReceived(frame -> {
                        Image calibratedImage = null;
                        if (calibration.isCalibrated()) {
                            calibratedImage = Capture.Mat2Image(calibration.calibrateImage(frame));
                        }
                        if (calibration.findAndDrawPoints(frame)) {
                            btnTakeSnapshot.setDisable(false);
                        } else {
                            btnTakeSnapshot.setDisable(true);
                        }
                        imgNormal.setImage(Capture.Mat2Image(frame));
                        imgCalibrated.setImage(calibratedImage);
                    });
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

        this.setOnHiding(e -> {
            try {
                capture.stop();
            } catch (Capture.CaptureException ignored) {}
        });
    }
}
