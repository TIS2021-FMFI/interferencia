package com.fmph.kai;

import com.fmph.kai.camera.Capture;
import com.fmph.kai.gui.CameraCalibrationWindow;
import com.fmph.kai.gui.ImageCanvas;
import com.fmph.kai.util.Compute;
import com.fmph.kai.util.ExceptionHandler;
import com.fmph.kai.util.Vector2D;
import com.fmph.kai.util.Formula;
import com.fmph.kai.util.MPoint;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.Core;
import java.io.*;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public class InterferenceApplication extends Application {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private final double width = 1350;
    private final double height = 900;

    private Group root;
    private Stage stage;
    private Scene scene;

    private Capture capture;
    private Compute compute;

    TextArea textArea;

    @Override
    public void start(Stage stage) {
        compute = new Compute();
        System.out.println("Using OpenCV v" + Core.VERSION);
        System.out.println("Using JavaFX v" + com.sun.javafx.runtime.VersionInfo.getVersion());
        this.stage = stage;
        root = new Group();
        scene = new Scene(root, width, height);
        initializeGUI();
        stage.setTitle("Interference analyzer");

        stage.setScene(scene);
        stage.setScene(scene);
        stage.show();

    }

    private void calculateRs()
    {
        double pixelDistance = Math.sqrt((compute.x1Len - compute.x2Len) * (compute.x1Len - compute.x2Len) +
                                         (compute.y1Len - compute.y2Len) * (compute.y1Len - compute.y2Len));
        double mmDistance = compute.lengthLen;


        Formula formula = new Formula();
        textArea.clear();
        for (int i = 0; i < compute.numMaxima - 1; i++) {
            MPoint p = compute.maxlist.get(i);
            double x1 = Math.sqrt((p.x - compute.x1) * (p.x - compute.x1) + (p.y - compute.y1) * (p.y - compute.y1));
            p = compute.maxlist.get(i + 1);
            double x2 = Math.sqrt((p.x - compute.x1) * (p.x - compute.x1) + (p.y - compute.y1) * (p.y - compute.y1));

            x1 = x1 / pixelDistance * mmDistance;
            x2 = x2 / pixelDistance * mmDistance;
            formula.d = compute.dHandler;
            double r = formula.finalR(x1, x2);
            textArea.appendText((i + 1)+ ": x1=" + String.format("%.4f", x1) + ", x2=" + String.format("%.4f", x2) + ", R=" + String.format("%.4f", r) + "\n");
        }

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
        MenuItem startCaptureMenuItem = new MenuItem("Start capture");
        MenuItem setLineSizeMenuItem = new MenuItem("Line size");
        MenuItem resetLineMenuItem = new MenuItem("Reset line");
        MenuItem cameraCalibrationMenuItem = new MenuItem("Camera calibration");
        fileMenu.getItems().addAll(openMenuItem, cameraCalibrationMenuItem, startCaptureMenuItem);
        editMenu.getItems().addAll(setLineSizeMenuItem, resetLineMenuItem);
        menu.getMenus().addAll(fileMenu, editMenu);
        borderPane.setTop(menu);

        // Graph
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("X");
        yAxis.setLabel("Y");
        yAxis.setAutoRanging(false);
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setMaxWidth(width/2);
        lineChart.prefHeight(1000);
        lineChart.setMaxHeight(height/2);
        VBox vboxGrafOutput = new VBox(10);
        vboxGrafOutput.getChildren().add(lineChart);

        // Output box
        textArea = TextAreaBuilder.create()
                .prefWidth(650)   //change here
                .wrapText(true)
                .build();
        vboxGrafOutput.getChildren().add(textArea);
        textArea.setEditable(false);
        borderPane.setRight(vboxGrafOutput);

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
        btnSubmitLineLength.setPrefWidth(100);
        HBox hboxCanvasTop = new HBox(5);
        hboxCanvasTop.getChildren().addAll(txtLineLength, btnSubmitLineLength);
        Label lblLineThickness = new Label("Line thickness");
        Slider sldLineThickness = new Slider();
        sldLineThickness.setMin(1);
        sldLineThickness.setMax(9);
        sldLineThickness.setShowTickLabels(true);
        sldLineThickness.setShowTickMarks(true);
        sldLineThickness.setMajorTickUnit(1);
        Label lblSliderValue = new Label(Double.toString(sldLineThickness.getValue()));
        sldLineThickness.valueProperty().addListener(
                (observable, oldValue, newValue) ->
                { lblSliderValue.setText(String.format("%.0f", newValue));
                  compute.lineWidth = (int)(0.5 + newValue.doubleValue()); });
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
        VBox vboxCalculation = new VBox(10);
        vboxCalculation.setPadding(new Insets(5));
        vboxCalculation.setStyle("-fx-border-color: silver");
        Label lblParameters = new Label("Setup parameters: D=");
        TextField txtPar1 = new TextField();
        txtPar1.setPrefWidth(50);
        Label lblParameters2 = new Label("?=");
        TextField txtPar2 = new TextField();
        txtPar2.setPrefWidth(50);
        Label lblParameters3 = new Label("?=");
        TextField txtPar3 = new TextField();
        txtPar3.setPrefWidth(50);
        Button btnSubmitParameters = new Button("Submit");
        HBox hboxCalculation1 = new HBox(15);
        hboxCalculation1.getChildren().addAll(lblParameters, txtPar1, lblParameters2, txtPar2, lblParameters3, txtPar3, btnSubmitParameters);
        Button btnGenerateMinMax = new Button("Find MIN/MAX R");
        btnGenerateMinMax.setPrefWidth(200);
        Button btnGeneratePointCloud = new Button("Save data");
        btnGeneratePointCloud.setPrefWidth(200);
        HBox hboxCalculation2 = new HBox(15);
        hboxCalculation2.getChildren().addAll(btnGenerateMinMax, btnGeneratePointCloud);
        vboxCalculation.getChildren().addAll(hboxCalculation1, hboxCalculation2);

        btnSubmitParameters.setOnAction(e -> {
            try {
                compute.dHandler = Double.valueOf(txtPar1.getText());
            } catch (NumberFormatException | NoSuchElementException exception) {
                ExceptionHandler.handle(exception);
            }
        });

        bottom.getChildren().addAll(
                vboxImage,
                hboxCanvas,
                vboxCalculation
                );
        borderPane.setBottom(bottom);

        // ImageCanvas
        ImageCanvas imageCanvas = new ImageCanvas(width/2, height-200, compute);
        imageCanvas.heightProperty().bind(vboxGrafOutput.heightProperty());
        imageCanvas.reset();
        borderPane.setLeft(imageCanvas);

        // Actions
        openMenuItem.setOnAction(e -> {
            File file = getImageFromFilesystem();
            if (file != null) {
                imageCanvas.setImage(new Image(file.toURI().toString()));
            }
        });


        imageCanvas.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && imageCanvas.leftClick(new Vector2D(e.getX(), e.getY()))) {
                if (compute.lengthLen > 0) {
                    // Ask for number of maximums
                    TextInputDialog tid = new TextInputDialog();
                    tid.setHeaderText("Enter the number of maximums to be analyzed:");

                    Optional<String> stringMaximums = tid.showAndWait();
                    try {
                        compute.numMaxima = Integer.parseInt(stringMaximums.get());
                    } catch (NumberFormatException | NoSuchElementException exception) {
                        ExceptionHandler.handle(exception);
                    }

                    compute.colorize(imageCanvas.getImage());
                    double yborder = (compute.ymax - compute.ymin) * 0.1;
                    yAxis.setLowerBound(compute.ymin - yborder);
                    yAxis.setUpperBound(compute.ymax + yborder);

                    compute.analyze();

                    // Redraw the graph
                    lineChart.getData().clear();
                    XYChart.Series<Number, Number> series = new XYChart.Series<>();
                    series.setName("Interference");
                    for (MPoint p : compute.pointlist) {
                        series.getData().add(new XYChart.Data<>(p.seq, p.suc));
                        //series.getData().add(new XYChart.Data<>(p.seq, p.suc*10-10));
                    }
                    XYChart.Series<Number, Number> maxes = new XYChart.Series<>();
                    maxes.setName("Maxima");

                    for (MPoint p : compute.maxlist) {
                        maxes.getData().add(new XYChart.Data<>(p.seq, p.suc));
                        //maxes.getData().add(new XYChart.Data<>(p.seq, p.suc*10-10));
                    }
                    //for (double x = 0; x < 5*Math.PI; x += Math.PI/24) {
                    //    series.getData().add(new XYChart.Data<>(x, Math.sin(x)));
                    //}

                    lineChart.getData().add(series);
                    lineChart.getData().add(maxes);
                    maxes.nodeProperty().get().setStyle("-fx-stroke: transparent;");
                    Set<Node> lookupAll = lineChart.lookupAll(".series1.chart-line-symbol");
                    for (Node n : lookupAll) {
                        n.setStyle("-fx-background-color: #00AA00, #AA0000");
                    }
                    calculateRs();

                } else {
                    compute.lengthClickLen = Math.sqrt((double) (compute.clickLenX1-compute.clickLenX2)*(compute.clickLenX1-compute.clickLenX2) + (double) (compute.clickLenY1-compute.clickLenY2)*(compute.clickLenY1-compute.clickLenY2));
                    // Ask for number of maximums
                    TextInputDialog tid = new TextInputDialog();
                    tid.setHeaderText("Enter line lenght in milimeters");
                    Integer inp = null; // <- use this value for the calculations
                    Optional<String> lineLength = tid.showAndWait();
                    try {
                        inp = Integer.parseInt(lineLength.get());
                        if (inp <= 0) {
                            throw new NumberFormatException("Number can not be negative");
                        }
                        compute.lengthLen = (double) inp;
                    } catch (NumberFormatException | NoSuchElementException exception) {
                        ExceptionHandler.handle(exception);
                    }
                }

            }
        });

        imageCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                imageCanvas.rightPressed(new Vector2D(e.getX(), e.getY()));
            }
        });

        imageCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                imageCanvas.rightDragged(new Vector2D(e.getX(), e.getY()));
            }
        });

        imageCanvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                imageCanvas.rightReleased(new Vector2D(e.getX(), e.getY()));
            }
        });

        imageCanvas.setOnScroll(e -> {
            imageCanvas.zoom(e.getDeltaY(), new Vector2D(e.getX(), e.getY()));
        });

        setLineSizeMenuItem.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog();
            tid.setHeaderText("Enter new line size:");
            tid.setOnHidden(event -> {
                // lineSize = Double.parseDouble(tid.getEditor().getText());
            });
            tid.show();
        });

        setLineSizeMenuItem.setOnAction(e -> {

        });

        resetLineMenuItem.setOnAction(e -> {
            imageCanvas.resetLine();
            //need to reset line Xs and Ys!!
        });

        cameraCalibrationMenuItem.setOnAction(e -> {
            try {
                CameraCalibrationWindow cameraCalibrationWindow = new CameraCalibrationWindow(stage.getX() + 20, stage.getY() + 20, width - 40, height - 40);
                cameraCalibrationWindow.show();
            } catch (Capture.CaptureException exception) {
                ExceptionHandler.handle(exception);
            }
        });

        btnUploadImage.setOnAction(e -> {
            File file = getImageFromFilesystem();
            if (file != null) {
                imageCanvas.setImage(new Image(file.toURI().toString()));
            }
        });

        btnCalibration.setOnAction(e -> {
            File file = getImageFromFilesystem();
            if (file != null) {
                //do smth with calibration file, idk what
            }
        });

        capture = new Capture();
        btnReadCamera.setOnAction(e -> {
            try {
                if (capture.isCapturing()) {
                    startCaptureMenuItem.setText("Start capture");
                    capture.stop();
                    return;
                }
                ObservableList<Integer> cameraIndexes = Capture.getAvailableCameras();
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
                    try {
                        capture.start(cameraIndex);
                    } catch (Capture.CaptureException exception) {
                        ExceptionHandler.handle(exception);
                    }
                    startCaptureMenuItem.setText("Stop capture");
                    capture.setOnFrameReceived(frame -> imageCanvas.setImage(Capture.Mat2Image(frame)));
                });
            } catch (Capture.CaptureException exception) {
                ExceptionHandler.handle(exception);
            }
        });
      
        root.getChildren().add(borderPane);
    }



    private File getImageFromFilesystem() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a picture");
        fileChooser.setInitialDirectory(
                new File(System.getProperty("user.home"))
        );
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Images", "*.jpg", "*.png", "*.bmp"),
                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("PNG", "*.png")
        );
        return fileChooser.showOpenDialog(stage);
    }

    public static void main(String[] args) {
        launch();
    }
}