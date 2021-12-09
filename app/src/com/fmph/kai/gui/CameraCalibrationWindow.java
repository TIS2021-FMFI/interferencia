package com.fmph.kai.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.opencv.videoio.Videoio.*;

public class CameraCalibrationWindow extends Stage {
    private final VideoCapture capture = new VideoCapture();
    private ScheduledExecutorService timer;
    private boolean isCalibrated = false;

    // calibration parameters
    private final Mat intrinsic = new Mat(3, 3, CvType.CV_32FC1);
    private final Mat distCoeffs = new Mat();
    private Image calibratedImage = null;
    private final MatOfPoint3f obj = new MatOfPoint3f();
    private final List<Mat> imagePoints = new ArrayList<>();
    private final List<Mat> objectPoints = new ArrayList<>();
    private MatOfPoint2f imageCorners = new MatOfPoint2f();
    private Size size;

    private int numRequired = 20;
    private int numSnapshots = 0;
    private int horSize = 9;
    private int verSize = 6;

    private Button btnTakeSnapshot;

    public CameraCalibrationWindow(double x, double y, double width, double height) {
        super();
        this.setTitle("Camera calibration");

        BorderPane pane = new BorderPane();

        // TOP
        HBox top = new HBox(10);
        top.setPadding(new Insets(40, 40, 40, 40));
        top.setAlignment(Pos.CENTER);
        Label lblSelectCamera = new Label("Select camera: ");
        SpinnerValueFactory<Integer> valueFactory = //
                new SpinnerValueFactory.IntegerSpinnerValueFactory(-1, 3, 0);
        Spinner<Integer> spnCamera = new Spinner<>();
        spnCamera.setValueFactory(valueFactory);
        Label lblBoards = new Label("Samples #");
        TextField txtBoards = new TextField("20");
        txtBoards.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.isEmpty())
                try {
                    numRequired = Integer.parseInt(newV);
                    if (btnTakeSnapshot != null)
                        btnTakeSnapshot.setText("Take snapshot (" + numSnapshots + "/" + numRequired + ")");
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
        });
        txtBoards.setMaxWidth(50);
        Label lblHorizontalCorners = new Label("Horizontal corners #");
        TextField txtHorizontalCorners = new TextField("9");
        txtHorizontalCorners.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.isEmpty())
                try {
                    horSize = Integer.parseInt(newV);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            updateSettings();
        });
        txtHorizontalCorners.setMaxWidth(50);
        Label lblVerticalCorners = new Label("Vertical corners #");
        TextField txtVerticalCorners = new TextField("6");
        txtVerticalCorners.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.isEmpty())
                try {
                    verSize = Integer.parseInt(newV);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            updateSettings();
        });
        txtVerticalCorners.setMaxWidth(50);
        top.getChildren().addAll(lblSelectCamera, spnCamera, lblBoards, txtBoards, lblHorizontalCorners, txtHorizontalCorners, lblVerticalCorners, txtVerticalCorners);
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
        btnTakeSnapshot = new Button("Take snapshot");
        btnTakeSnapshot.setDisable(true);
        bottom.getChildren().addAll(btnStartCamera, btnTakeSnapshot);
        pane.setBottom(bottom);

        // SCENE
        Scene scene = new Scene(pane, width, height);
        this.setScene(scene);

        this.setX(x);
        this.setY(y);

        // VideoCapture
        btnStartCamera.setOnAction(e -> {
            if (capture.isOpened()) {
                btnStartCamera.setText("Start camera");
                if (timer != null)
                    timer.shutdown();
                capture.release();
                imgNormal.setImage(null);
                imgCalibrated.setImage(null);
            } else {
                if (!capture.open(spnCamera.getValue() + CAP_DSHOW)) {
                    System.out.println("Error opening camera!");
                    return;
                }
                btnStartCamera.setText("Stop camera");
                Runnable frameGrabber = () -> {
                    Image imageToShow = grabFrame();
                    Platform.runLater(() -> {
                        imgNormal.setImage(imageToShow);
                        imgCalibrated.setImage(calibratedImage);
                    });
                };
                timer = Executors.newSingleThreadScheduledExecutor();
                timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
            }
        });

        // Calibration
        updateSettings();
        btnTakeSnapshot.setOnAction(e -> {
            if (numSnapshots < numRequired) {
                numSnapshots++;
                imagePoints.add(imageCorners.clone());
                imageCorners = new MatOfPoint2f();
                objectPoints.add(obj.clone());
                btnTakeSnapshot.setText("Take snapshot (" + numSnapshots + "/" + numRequired + ")");
                if (numSnapshots == numRequired)
                    calibrate();
            } else {
                calibrate();
            }
        });

        this.setOnHiding(e -> {
            if (timer != null)
                timer.shutdown();
            capture.release();
        });
    }

    private Image grabFrame() {
        if (capture.isOpened()) {
            Mat frame = new Mat();
            capture.read(frame);
            if (isCalibrated)
            {
                Mat undistorted = new Mat();
                Calib3d.undistort(frame, undistorted, intrinsic, distCoeffs);
                calibratedImage = mat2Image(undistorted);
            }
            findAndDrawPoints(frame);
            return mat2Image(frame);
        }
        return null;
    }

    private void findAndDrawPoints(Mat frame) {
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Size boardSize = new Size(horSize, verSize);
        boolean found = Calib3d.findChessboardCorners(gray, boardSize, imageCorners, Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK);
        if (found) {
            TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 30, 0.1);
            Imgproc.cornerSubPix(gray, imageCorners, new Size(11, 11), new Size(-1, -1), term);
            size = gray.size();
            Calib3d.drawChessboardCorners(frame, boardSize, imageCorners, true);
            btnTakeSnapshot.setDisable(false);
        } else {
            btnTakeSnapshot.setDisable(true);
        }
    }

    private void calibrate() {
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();
        intrinsic.put(0, 0, 1);
        intrinsic.put(1, 1, 1);
        Calib3d.calibrateCamera(objectPoints, imagePoints, size, intrinsic, distCoeffs, rvecs, tvecs);
        isCalibrated = true;

        saveCalibration(intrinsic, distCoeffs);

        btnTakeSnapshot.setDisable(true);
    }

    private Image mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    private void updateSettings() {
        for (double j = 0; j < horSize * verSize; j++)
            obj.push_back(new MatOfPoint3f(new Point3(j / horSize, j % verSize, 0.0f)));
    }

    private void saveCalibration(Mat intrinsic, Mat distCoeffs) {
        saveDoubleMat(intrinsic, "intrinsic");
        saveDoubleMat(distCoeffs, "distortion");
    }

    // TODO: loadDoubleMat?
    private void saveDoubleMat(final Mat mat, final String fileName) {
        final long count = mat.total() * mat.channels();
        final double[] buff = new double[(int) count];
        mat.get(0, 0, buff);
        try (final DataOutputStream out = new DataOutputStream(new FileOutputStream(fileName))) {
            for (double v : buff) {
                out.writeDouble(v);
            }
        } catch (IOException e) {
            // TODO: introduce an exception handling system
            System.out.println("Error saving the matrix: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Error saving the calibration!");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
