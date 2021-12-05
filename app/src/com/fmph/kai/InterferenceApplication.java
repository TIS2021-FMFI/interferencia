package com.fmph.kai;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.Core;

import java.io.File;

public class InterferenceApplication extends Application {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private final double width = 1366;
    private final double height = 768;

    private Group root;
    private Stage stage;
    private Scene scene;

    private double lineSize;

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
        MenuItem setLineSizeMenuItem = new MenuItem("Line size");
        MenuItem resetLineMenuItem = new MenuItem("Reset line");
        MenuItem cameraCalibrationMenuItem = new MenuItem("Camera calibration");
        fileMenu.getItems().addAll(openMenuItem, cameraCalibrationMenuItem);
        editMenu.getItems().addAll(setLineSizeMenuItem, resetLineMenuItem);
        menu.getMenus().addAll(fileMenu, editMenu);

        // Split Pane
        SplitPane splitPane = new SplitPane();
        splitPane.setLayoutY(25);
        splitPane.orientationProperty().setValue(Orientation.HORIZONTAL);
        ImageCanvas imageCanvas = new ImageCanvas(width/2, height-200);

        // Graph
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("X");
        yAxis.setLabel("Y");
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setMaxWidth(width/2);

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
                lineSize = Double.parseDouble(tid.getEditor().getText());
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

        //first horizontal line
        Label SliderCaption = new Label("Line thickness slider");
        Label ParametresCaption = new Label("Setup parametres");

        HBox layout1 = new HBox(120);
        layout1.setPadding(new Insets(height-165, 20,20,450));
        layout1.getChildren().addAll(SliderCaption, ParametresCaption);

        //second horizontal line
        TextField lenghtLine = new TextField("Enter the lenght of the line");
        Button submit = new Button("Submit");
        CheckBox selectLine = new CheckBox("Select line");
        CheckBox selectPoint = new CheckBox("Select point");
        Slider slider = new Slider();
        slider.setMin(0);
        slider.setMax(5);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(1);

        Label SliderValue = new Label(Double.toString(slider.getValue()));
        slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                SliderValue.setText(String.format("%.2f", newValue));
            }
        });

        HBox layout2 = new HBox(10);
        layout2.setPadding(new Insets(height-160,20,20,20));
        layout2.setLayoutY(25);
        layout2.getChildren().addAll(lenghtLine,submit,selectLine,selectPoint,slider,SliderValue);

        //third horizontal from right side
        TextField par1 = new TextField();
        par1.setPrefWidth(30);
        TextField par2 = new TextField();
        par2.setPrefWidth(30);
        TextField par3 = new TextField();
        par3.setPrefWidth(30);
        Button submitPar = new Button("Submit");
        Button genMaxMIn = new Button("Generate global max/min R");
        Button pointCloud = new Button("Create point cloud file");


        HBox layout3 = new HBox(10);
        layout3.setPadding(new Insets(height-160,20,20,width-700));
        layout3.setLayoutY(25);
        layout3.getChildren().addAll(par1,par2,par3, submitPar, genMaxMIn, pointCloud);




        root.getChildren().addAll(menu, splitPane,layout1, layout2, layout3);
    }

    public static void main(String[] args) {
        launch();
    }
}