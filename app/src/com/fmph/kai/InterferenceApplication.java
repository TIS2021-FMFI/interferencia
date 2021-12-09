package com.fmph.kai;

import com.fmph.kai.gui.CameraCalibrationWindow;
import com.fmph.kai.gui.ImageCanvas;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.opencv.videoio.Videoio.CAP_DSHOW;

public class InterferenceApplication extends Application {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private final double width = 1366;
    private final double height = 768;

    private Group root;
    private Stage stage;
    private Scene scene;

    private final VideoCapture capture = new VideoCapture();
    private ScheduledExecutorService timer;
    boolean capturing = false;

    @Override
    public void start(Stage stage) {
        System.out.println("Using OpenCV v" + Core.VERSION);
        System.out.println("Using JavaFX v" + com.sun.javafx.runtime.VersionInfo.getVersion());
        this.stage = stage;
        root = new Group();
        scene = new Scene(root, width, height);
        initializeGUI();
        stage.setTitle("Interference analyzer");
        stage.setScene(scene);
        stage.show();
    }

    private void initializeGUI() {
        // Menu Bar
        MenuBar menu = new MenuBar();
        Menu fileMenu = new Menu("File");
        Menu editMenu = new Menu("Edit");
        MenuItem openMenuItem = new MenuItem("Open");
        MenuItem startCaptureMenuItem = new MenuItem("Start capture");
        MenuItem setLineSizeMenuItem = new MenuItem("Line size");
        MenuItem resetLineMenuItem = new MenuItem("Reset line");
        MenuItem cameraCalibrationMenuItem = new MenuItem("Camera calibration");
        fileMenu.getItems().addAll(openMenuItem, cameraCalibrationMenuItem, startCaptureMenuItem);
        editMenu.getItems().addAll(setLineSizeMenuItem, resetLineMenuItem);
        menu.getMenus().addAll(fileMenu, editMenu);

        // Split Pane
        SplitPane splitPane = new SplitPane();
        splitPane.setLayoutY(25);
        splitPane.orientationProperty().setValue(Orientation.HORIZONTAL);
        ImageCanvas imageCanvas = new ImageCanvas(2*width/3, height - 25);

        // Graph
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("X");
        yAxis.setLabel("Y");
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setMaxWidth(width/3);

        splitPane.getItems().addAll(imageCanvas, lineChart);

        // Actions
        openMenuItem.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose a picture");
            fileChooser.setInitialDirectory(
                    new File(System.getProperty("user.home"))
            );
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All Images", "*.*"),
                    new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                    new FileChooser.ExtensionFilter("PNG", "*.png")
            );
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                imageCanvas.setImage(new Image(file.toURI().toString()));
            }
        });

        imageCanvas.setOnMouseClicked(e -> {
            if (imageCanvas.click(e.getX(), e.getY())) {
                lineChart.getData().clear();
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName("Sinusoid");
                for (double x = 0; x < 5*Math.PI; x += Math.PI/24) {
                    series.getData().add(new XYChart.Data<>(x, Math.sin(x)));
                }
                lineChart.getData().add(series);
            }
        });

        setLineSizeMenuItem.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog();
            tid.setHeaderText("Enter new line size:");
            tid.setOnHidden(event -> {
                //lineSize = Double.parseDouble(tid.getEditor().getText());
            });
            tid.show();
        });

        resetLineMenuItem.setOnAction(e -> {
            imageCanvas.resetLine();
        });

        cameraCalibrationMenuItem.setOnAction(e -> {
            CameraCalibrationWindow cameraCalibrationWindow = new CameraCalibrationWindow(stage.getX() + 20, stage.getY() + 20, width - 40, height - 40);
            cameraCalibrationWindow.show();
        });

        startCaptureMenuItem.setOnAction(e -> {
            if (capturing) {
                startCaptureMenuItem.setText("Start capture");
                capturing = false;
                timer.shutdown();
                capture.release();
                return;
            }
            ProcessBuilder processBuilder = new ProcessBuilder("python", "app/src/com/fmph/kai/camera_test.py");
            ObservableList<Integer> cameraIndexes = FXCollections.observableArrayList();
            try {
                Process p = processBuilder.start();

                BufferedReader stdInput = new BufferedReader(new
                        InputStreamReader(p.getInputStream()));

                BufferedReader stdError = new BufferedReader(new
                        InputStreamReader(p.getErrorStream()));

                String s;
                while ((s = stdInput.readLine()) != null) {
                    cameraIndexes.add(Integer.parseInt(s));
                }

                while ((s = stdError.readLine()) != null) {
                    // TODO: handle errors better
                    System.out.println(s);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Dialog<Integer> dialog = new Dialog<>();
            dialog.setTitle("Choose camera");

            ButtonType submitButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

            ComboBox<Integer> cameraIndexComboBox = new ComboBox<>();
            cameraIndexComboBox.setItems(cameraIndexes);
            cameraIndexComboBox.setValue(0);

            dialog.getDialogPane().setContent(cameraIndexComboBox);

            Platform.runLater(cameraIndexComboBox::requestFocus);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == submitButtonType) {
                    return cameraIndexComboBox.getValue();
                }
                return null;
            });

            Optional<Integer> result = dialog.showAndWait();

            result.ifPresent(cameraIndex -> {
                if (!capture.open(cameraIndex + CAP_DSHOW)) {
                    // TODO: need better exception handling
                    System.out.println("Error opening camera!");
                    return;
                }
                capturing = true;
                startCaptureMenuItem.setText("Stop capture");
                Runnable frameGrabber = () -> {
                    Image imageToShow = grabFrame();
                    Platform.runLater(() -> {
                        if (capturing)
                            imageCanvas.setImage(imageToShow);
                    });
                };
                timer = Executors.newSingleThreadScheduledExecutor();
                timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
            });
        });

        root.getChildren().addAll(menu, splitPane);
    }

    private Image grabFrame() {
        if (capture.isOpened()) {
            Mat frame = new Mat();
            capture.read(frame);
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".png", frame, buffer);
            return new Image(new ByteArrayInputStream(buffer.toArray()));
        }
        return null;
    }

    public static void main(String[] args) {
        launch();
    }
}