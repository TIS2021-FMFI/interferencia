package com.fmph.kai;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.Core;

import java.io.File;

public class InterferenceApplication extends Application {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private final double width = 900;
    private final double height = 800;

    private Group root;
    private Stage stage;
    private Scene scene;

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
        // Border Pane
        BorderPane borderPane = new BorderPane();
        borderPane.setPrefWidth(width);
        borderPane.setPrefHeight(height);

        // Menu Bar
        MenuBar menu = new MenuBar();
        Menu fileMenu = new Menu("File");
        Menu editMenu = new Menu("Edit");
        MenuItem openMenuItem = new MenuItem("Open");
        MenuItem setLineSizeMenuItem = new MenuItem("Line size");
        MenuItem resetLineMenuItem = new MenuItem("Reset line");
        MenuItem cameraCalibrationMenuItem = new MenuItem("Camera calibration");
        fileMenu.getItems().addAll(openMenuItem, cameraCalibrationMenuItem);
        editMenu.getItems().addAll(setLineSizeMenuItem, resetLineMenuItem);
        menu.getMenus().addAll(fileMenu, editMenu);
        borderPane.setTop(menu);

        // ImageCanvas
        ImageCanvas imageCanvas = new ImageCanvas(width/2, height-200);
        borderPane.setLeft(imageCanvas);

        // Graph
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("X");
        yAxis.setLabel("Y");
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setMaxWidth(width/2);
        borderPane.setRight(lineChart);

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
                // lineSize = Double.parseDouble(tid.getEditor().getText());
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

        // Bottom pane
        HBox bottom = new HBox(10);
        bottom.setPadding(new Insets(5));

        // Image tools
        VBox vboxImage = new VBox(10);
        vboxImage.setPadding(new Insets(5));
        vboxImage.setStyle("-fx-border-color: silver");
        HBox hboxImage1 = new HBox(10);
        HBox hboxImage2 = new HBox(10);
        Button btnReadCamera = new Button("Read from camera");
        btnReadCamera.setPrefWidth(120);
        Button btnUploadImage = new Button("Upload the image");
        btnUploadImage.setPrefWidth(120);
        Button btnCalibration = new Button("Select calibration file");
        btnCalibration.setPrefWidth(120);
        CheckBox chkCalibration = new CheckBox("use the calibration");
        hboxImage1.getChildren().addAll(btnReadCamera, btnUploadImage);
        hboxImage2.getChildren().addAll(btnCalibration, chkCalibration);
        vboxImage.getChildren().addAll(hboxImage1, hboxImage2);

        // Canvas tools
        HBox hboxCanvas = new HBox(10);
        hboxCanvas.setPadding(new Insets(5));
        hboxCanvas.setStyle("-fx-border-color: silver");
        TextField txtLineLength = new TextField("Line length");
        Button btnSubmitLineLength = new Button("Submit");
        btnSubmitLineLength.setPrefWidth(90);
        HBox hboxCanvasTop = new HBox(5);
        hboxCanvasTop.getChildren().addAll(txtLineLength, btnSubmitLineLength);
        Label lblLineThickness = new Label("Line thickness");
        Slider sldLineThickness = new Slider();
        sldLineThickness.setMin(0);
        sldLineThickness.setMax(5);
        sldLineThickness.setShowTickLabels(true);
        sldLineThickness.setShowTickMarks(true);
        sldLineThickness.setMajorTickUnit(1);
        Label lblSliderValue = new Label(Double.toString(sldLineThickness.getValue()));
        sldLineThickness.valueProperty().addListener((observable, oldValue, newValue) -> lblSliderValue.setText(String.format("%.2f", newValue)));
        HBox hboxCanvasBottom = new HBox(5);
        hboxCanvasBottom.getChildren().addAll(lblLineThickness, sldLineThickness, lblSliderValue);
        VBox vboxCanvasLeft = new VBox(5);
        vboxCanvasLeft.getChildren().addAll(hboxCanvasTop, hboxCanvasBottom);
        ToggleGroup toggleGroup = new ToggleGroup();
        RadioButton rdbSelectLine = new RadioButton("Select line");
        rdbSelectLine.setToggleGroup(toggleGroup);
        RadioButton rdbSelectPoint = new RadioButton("Select point");
        rdbSelectPoint.setToggleGroup(toggleGroup);
        VBox vboxCanvasRight = new VBox(5);
        vboxCanvasRight.getChildren().addAll(rdbSelectLine, rdbSelectPoint);
        hboxCanvas.getChildren().addAll(vboxCanvasLeft, vboxCanvasRight);

        // Calculation tools
        VBox vboxCalculation = new VBox(5);
        vboxCalculation.setPadding(new Insets(5));
        vboxCalculation.setStyle("-fx-border-color: silver");
        Label lblParameters = new Label("Setup parameters");
        TextField txtPar1 = new TextField();
        txtPar1.setPrefWidth(30);
        TextField txtPar2 = new TextField();
        txtPar2.setPrefWidth(30);
        TextField txtPar3 = new TextField();
        txtPar3.setPrefWidth(30);
        Button btnSubmitParameters = new Button("Submit");
        HBox hboxCalculation1 = new HBox(5);
        hboxCalculation1.getChildren().addAll(lblParameters, txtPar1, txtPar2, txtPar3, btnSubmitParameters);
        Button btnGenerateMinMax = new Button("Find MIN/MAX R");
        btnGenerateMinMax.setPrefWidth(128);
        Button btnGeneratePointCloud = new Button("Save data");
        btnGeneratePointCloud.setPrefWidth(128);
        HBox hboxCalculation2 = new HBox(5);
        hboxCalculation2.getChildren().addAll(btnGenerateMinMax, btnGeneratePointCloud);
        vboxCalculation.getChildren().addAll(hboxCalculation1, hboxCalculation2);

        bottom.getChildren().addAll(
                vboxImage,
                hboxCanvas,
                vboxCalculation
                );
        borderPane.setBottom(bottom);

        root.getChildren().add(borderPane);
    }

    public static void main(String[] args) {
        launch();
    }
}