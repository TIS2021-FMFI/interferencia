package com.fmph.kai;

import com.fmph.kai.camera.Calibration;
import com.fmph.kai.camera.Capture;
import com.fmph.kai.gui.CameraCalibrationWindow;
import com.fmph.kai.gui.ImageCanvas;
import com.fmph.kai.gui.ToggleSwitch;
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

/**
 * Main class of the application.
 */
public class InterferenceApplication extends Application {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private final double width = 1200;
    private final double height = 600;

    private Group root;
    private Stage stage;
    private Scene scene;

    private Capture capture;
    private Compute compute;
    private Calibration calibration;
    private boolean useCalibration;

    TextArea textArea;

    private ImageCanvas imageCanvas;
    private NumberAxis xAxis;
    private NumberAxis yAxis;
    private LineChart<Number, Number> lineChart;

    private String angleText;
    private double currentAngle;
    private double minR;
    private double maxR;
    private double angleForMinR;
    private double angleForMaxR;

    /**
     * Overrides the default start method.
     * Initializes and shows the main app window.
     * @param stage is the window
     */
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

    private void calculateRs() throws IOException
    {
        double pixelDistance = Math.sqrt((compute.x1Len - compute.x2Len) * (compute.x1Len - compute.x2Len) +
                                         (compute.y1Len - compute.y2Len) * (compute.y1Len - compute.y2Len));
        double mmDistance = compute.lengthLen;


        Formula formula = new Formula();
        formula.lambda = compute.lambdaHandler;
        formula.d = compute.dHandler;

        int numIterations = Math.min(compute.numIterations, compute.maxlist.size() - 1);

        double averageR = 0;
        for (int i = 0; i < numIterations; i++) {
            MPoint p = compute.maxlist.get(i);
            double x1 = Math.sqrt((p.x - compute.x1) * (p.x - compute.x1) + (p.y - compute.y1) * (p.y - compute.y1));
            p = compute.maxlist.get(i + 1);
            double x2 = Math.sqrt((p.x - compute.x1) * (p.x - compute.x1) + (p.y - compute.y1) * (p.y - compute.y1));

            x1 = x1 / pixelDistance * mmDistance;
            x2 = x2 / pixelDistance * mmDistance;

            double r = formula.finalR(x1, x2);
            averageR += r;

            textArea.appendText((i + 1)+ ":" + angleText + " x1=" + String.format("%.4f", x1) + "; x2=" + String.format("%.4f", x2) + "; R=" + String.format("%.4f", r) + "\n");
            if(writer != null){
                double x11 = 0, y11 = 0;
                try {
                    x11 = (1 - (formula.d / (formula.d + (r / 2)))) * x1;
                    y11 = r - Math.sqrt(r * r - x11 * x11);
                }catch (Exception e){ }

                writer.write(String.format("%.16f ",(x11*Math.sin(currentAngle))));
                writer.write(String.format("%.16f ",(x11*Math.cos(currentAngle))));
                writer.write(String.format("%.16f ",(y11*Math.cos(currentAngle))));
                writer.newLine();
            }
        }

        averageR /= numIterations;
        if (averageR < minR) { minR = averageR; angleForMinR = currentAngle; }
        if (averageR > maxR) { maxR = averageR; angleForMaxR = currentAngle; }
        textArea.appendText("avg" + angleText + " R=" + String.format("%.4f",averageR) + "\n");


    }

    BufferedWriter writer = null;

    public void openWriter()
            throws IOException {
        writer = new BufferedWriter(new FileWriter(compute.scanHandler));
    }

    public void scanR()  {
        textArea.clear();
        double X2 = compute.clickLineX2;
        double Y2 = compute.clickLineY2;
        double angle = compute.alpha/ 180*Math.PI;
        maxR = -1;
        minR = Formula.UPPER_BOUND_FOR_R + 1;
        try {
            openWriter();
            for (int i = 0; i < 360 / compute.alpha; i++) {
                currentAngle = angle * i;
                angleText = " angle=" + String.format("%.4f", currentAngle) + ";";
                compute.clickLineX2 = (int) Math.round(compute.clickLineX1 + (X2 - compute.clickLineX1) * Math.cos(currentAngle) - (Y2 - compute.clickLineY1) * Math.sin(currentAngle));
                compute.clickLineY2 = (int) Math.round(compute.clickLineY1 + (X2 - compute.clickLineX1) * Math.sin(currentAngle) + (Y2 - compute.clickLineY1) * Math.cos(currentAngle));
                runComputationForSingleLine(false);
            }
            textArea.appendText("minR=" + String.format("%.4f", minR) + " for angle=" + String.format("%.4f", (angleForMinR / Math.PI * 180)) + "\n");
            textArea.appendText("maxR=" + String.format("%.4f", maxR) + " for angle=" + String.format("%.4f", (angleForMaxR / Math.PI * 180)) + "\n");
            writer.close();
        } catch (IOException ioe)
        {
            textArea.appendText("IOException when trying to create xyz-file " + ioe.getMessage());
        }

    }


    private void initializeGUI() {
        // Border Pane
        BorderPane borderPane = new BorderPane();
        borderPane.setPrefWidth(width);
        borderPane.setPrefHeight(height);
        borderPane.setMaxWidth(width);
        borderPane.setMaxHeight(height);
        borderPane.setMinWidth(width);
        borderPane.setMinHeight(height);


        // Menu Bar
        MenuBar menu = new MenuBar();
        Menu fileMenu = new Menu("File");
        Menu editMenu = new Menu("Edit");
        MenuItem openMenuItem = new MenuItem("Open");
        MenuItem startCaptureMenuItem = new MenuItem("Start capture");
        MenuItem resetLineMenuItem = new MenuItem("Reset line");
        MenuItem cameraCalibrationMenuItem = new MenuItem("Camera calibration");
        fileMenu.getItems().addAll(openMenuItem, cameraCalibrationMenuItem, startCaptureMenuItem);
        editMenu.getItems().addAll(resetLineMenuItem);
        menu.getMenus().addAll(fileMenu, editMenu);
        borderPane.setTop(menu);

        // Graph
        xAxis = new NumberAxis();
        yAxis = new NumberAxis();
        xAxis.setLabel("X");
        yAxis.setLabel("Y");
        yAxis.setAutoRanging(false);
        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setMaxWidth(width/2 );
        lineChart.prefHeight(1000);
        lineChart.setMaxHeight(height/2);
        VBox vboxGrafOutput = new VBox(10);
        vboxGrafOutput.setPadding(new Insets(10,10,10,10));
        vboxGrafOutput.setMaxWidth(width/2-20);
        vboxGrafOutput.setStyle("-fx-border-color: silver");
        vboxGrafOutput.getChildren().add(lineChart);

        // Output box
        textArea = TextAreaBuilder.create()
                .wrapText(true)
                .build();
        textArea.setMaxWidth(width/2-40);
        vboxGrafOutput.getChildren().add(textArea);
        textArea.setEditable(false);

        borderPane.setRight(vboxGrafOutput);

        // Bottom pane
        HBox bottom = new HBox(5);
        bottom.setPadding(new Insets(5));

        // Image tools
        calibration = new Calibration();
        useCalibration = false;
        VBox vboxImage = new VBox(5);
        vboxImage.setPadding(new Insets(15,5,10,5));
        vboxImage.setStyle("-fx-border-color: silver");
        HBox hboxImage1 = new HBox(10);
        Label lblCapture = new Label("Capture");
        ToggleSwitch tglCapture = new ToggleSwitch();
        tglCapture.setPrefWidth(40);
        Button btnUploadImage = new Button("Upload the image");
        btnUploadImage.setPrefWidth(120);
        Button btnCalibration = new Button("Select calibration file");
        btnCalibration.setPrefWidth(140);

        HBox hBoxCalib = new HBox(5);
        hBoxCalib.setPadding(new Insets(5,5,5,250));
        CheckBox chkCalibration = new CheckBox("use the calibration");
        chkCalibration.setDisable(true);
        hboxImage1.getChildren().addAll(lblCapture, tglCapture, btnUploadImage, btnCalibration);
        hBoxCalib.getChildren().addAll(chkCalibration);
        vboxImage.getChildren().addAll(hboxImage1, hBoxCalib);
        
        // Canvas tools
        HBox hboxCanvas = new HBox(5);
        hboxCanvas.setPadding(new Insets(5));
        hboxCanvas.setStyle("-fx-border-color: silver");
        HBox hboxCanvasTop = new HBox(5);

        Label lblLineThickness = new Label("Line thickness");
        Slider sldLineThickness = new Slider(1, 50, 5);
        sldLineThickness.setShowTickLabels(true);
        sldLineThickness.setShowTickMarks(true);
        sldLineThickness.setMajorTickUnit(1);
        Label lblSliderValue = new Label(Double.toString(sldLineThickness.getValue()));
        sldLineThickness.valueProperty().addListener(
                (observable, oldValue, newValue) ->
                { lblSliderValue.setText(String.format("%.0f", newValue));
                  compute.lineWidth = (int)(0.5 + newValue.doubleValue()); });
        Label lblParameterNumberMaxima = new Label("# of iterations =");
        TextField txtNumberMax = new TextField();
        txtNumberMax.setPrefWidth(50);
        HBox hboxCanvasBottom = new HBox(5);
        hboxCanvasBottom.getChildren().addAll(lblLineThickness, sldLineThickness, lblSliderValue);
        HBox hboxCanvasBottomMax = new HBox(5);
        hboxCanvasBottomMax.getChildren().addAll(lblParameterNumberMaxima,txtNumberMax);
        VBox vboxCanvasLeft = new VBox(5);
        vboxCanvasLeft.getChildren().addAll(hboxCanvasTop, hboxCanvasBottom,hboxCanvasBottomMax);
        hboxCanvas.getChildren().addAll(vboxCanvasLeft);

        // Calculation tools
        VBox vboxCalculation = new VBox(5);
        vboxCalculation.setPadding(new Insets(15,5,10,5));
        vboxCalculation.setStyle("-fx-border-color: silver");

        Label lblParameters = new Label("Setup parameters: D =");
        TextField txtD = new TextField();
        txtD.setPrefWidth(50);
        Label lblParameters2 = new Label("λ =");
        TextField txtLambda = new TextField();
        txtLambda.setPrefWidth(50);
        Label lblParameterAlfa = new Label("α =");
        TextField txtAlfa = new TextField();
        txtAlfa.setPrefWidth(50);
        Label lblParameterEpsilon = new Label("ε =");
        TextField txtEpsilon = new TextField();
        txtEpsilon.setPrefWidth(50);
        Label lblParameterMinX = new Label("minX =");
        TextField txtMinX = new TextField();
        txtMinX.setPrefWidth(50);
        Label lblParameterMinY = new Label("minY =");
        TextField txtMinY = new TextField();
        txtMinY.setPrefWidth(50);


        txtD.setText(Double.toString(compute.dHandler));
        txtLambda.setText(Double.toString(compute.lambdaHandler * 1000000));
        txtAlfa.setText(Double.toString(compute.alpha));
        txtEpsilon.setText(Double.toString(compute.chybickaHandler));
        txtMinX.setText(Double.toString(compute.maximumIgnoreBorderLeftHandler));
        txtMinY.setText(Double.toString(compute.maximumIgnoreBorderBottomHandler));
        txtNumberMax.setText(Integer.toString(compute.numIterations));

        Button btnSubmitParameters = new Button("Submit");
        btnSubmitParameters.setPrefWidth(120);

        HBox hboxCalculation1 = new HBox(5);
        HBox hboxDalsieP =  new HBox(5);
        hboxCalculation1.getChildren().addAll(lblParameters, txtD, lblParameters2, txtLambda, btnSubmitParameters);
        hboxDalsieP.getChildren().addAll(lblParameterAlfa,txtAlfa, lblParameterEpsilon, txtEpsilon, lblParameterMinX,txtMinX,lblParameterMinY,txtMinY);
        vboxCalculation.getChildren().addAll(hboxCalculation1, hboxDalsieP);

        //auto finding max min and 3D Scanning
        VBox vBoxScanning = new VBox(5);
        vBoxScanning.setPadding(new Insets(15,5,10,5));
        vBoxScanning.setStyle("-fx-border-color: silver");
        HBox hBoxAuto = new HBox(5);
        Button btnAutoMaxMin = new Button("Radial Scan");
        btnAutoMaxMin.setPrefWidth(140);
        hBoxAuto.getChildren().addAll(btnAutoMaxMin);
        HBox hBoxScan = new HBox(5);
        Label lblNazovSub = new Label("xyz: ");
        TextField txtNazovSub = new TextField();
        txtNazovSub.setPrefWidth(100);

        hBoxScan.getChildren().addAll(lblNazovSub,txtNazovSub);
        vBoxScanning.getChildren().addAll(hBoxAuto, hBoxScan);

        txtNazovSub.setText(compute.scanHandler);

        btnAutoMaxMin.setOnAction( event -> {

                scanR();

                }

        );

        btnSubmitParameters.setOnAction(e -> {
            try {
                double newD = Double.valueOf(txtD.getText());
                double newLambda = Double.valueOf(txtLambda.getText()) / 1000000;
                double newAlpha =  Double.valueOf(txtAlfa.getText());
                double newEpsilon =  Double.valueOf(txtEpsilon.getText());
                double newMinX =  Double.valueOf(txtMinX.getText());
                double newMinY =  Double.valueOf(txtMinY.getText());
                double newNumMaxima =  Double.valueOf(txtNumberMax.getText());

                compute.numIterations = (int) newNumMaxima;

                if ((newEpsilon >= Compute.MINIMUM_ACCEPTABLE_EPSILON) && (newEpsilon <= Compute.MAXIMUM_ACCEPTABLE_EPSILON))
                    compute.chybickaHandler = newEpsilon;
                if ((newMinX >= Compute.MINIMUM_ACCEPTABLE_LEFTBORDER) && (newMinX <= Compute.MAXIMUM_ACCEPTABLE_LEFTBORDER))
                    compute.maximumIgnoreBorderLeftHandler = newMinX;
                if ((newMinY >= Compute.MINIMUM_ACCEPTABLE_BOTTOMBORDER) && (newMinY <= Compute.MAXIMUM_ACCEPTABLE_BOTTOMBORDER))
                    compute.maximumIgnoreBorderBottomHandler = newMinY;
                if ((newAlpha >= Compute.MINIMUM_ACCEPTABLE_A) && (newAlpha <= Compute.MAXIMUM_ACCEPTABLE_A))
                    compute.alpha = newAlpha;
                if ((newD >= Compute.MINIMUM_ACCEPTABLE_D) && (newD <= Compute.MAXIMUM_ACCEPTABLE_D))
                   compute.dHandler = newD;
                if ((newLambda >= Compute.MINIMUM_ACCEPTABLE_LAMBDA) && (newLambda <= Compute.MAXIMUM_ACCEPTABLE_LAMBDA))
                   compute.lambdaHandler = newLambda;
                angleText = "";
                try { runComputationForSingleLine(true); } catch (IOException ioe) {}
            } catch (NumberFormatException | NoSuchElementException exception) {
                ExceptionHandler.handle(exception);
            }
            txtD.setText(Double.toString(compute.dHandler));
            txtLambda.setText(Double.toString(compute.lambdaHandler * 1000000));
            txtAlfa.setText(Double.toString(compute.alpha));
            txtEpsilon.setText(Double.toString(compute.chybickaHandler));
            txtMinX.setText(Double.toString(compute.maximumIgnoreBorderLeftHandler));
            txtMinY.setText(Double.toString(compute.maximumIgnoreBorderBottomHandler));

        });

        bottom.getChildren().addAll(
                vboxImage,
                hboxCanvas,
                vboxCalculation,
                vBoxScanning
                );
        borderPane.setBottom(bottom);

        // ImageCanvas
        imageCanvas = new ImageCanvas(width/2, height-200, compute);
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
                    angleText = "";
                    try { runComputationForSingleLine(true); } catch (IOException ioe) {}


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

        resetLineMenuItem.setOnAction(e -> {
            imageCanvas.resetLine();
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
            loadCalibrationFiles();
            chkCalibration.setDisable(false);
        });

        chkCalibration.selectedProperty().addListener((obs, oldValue, newValue) -> {
            useCalibration = newValue;
        });

        capture = new Capture();
        tglCapture.setOnEnabled(() -> {
            try {
                if (capture.isCapturing()) {
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
                    capture.setOnFrameReceived(frame -> {
                        if (useCalibration) {
                            imageCanvas.setImage(Capture.Mat2Image(calibration.calibrateImage(frame)));
                        } else {
                            imageCanvas.setImage(Capture.Mat2Image(frame));
                        }
                    });
                });
            } catch (Capture.CaptureException exception) {
                ExceptionHandler.handle(exception);
            }
        });
        tglCapture.setOnDisabled(() -> {
            try {
                if (!capture.isCapturing()) {
                    return;
                }
                startCaptureMenuItem.setText("Start capture");
                capture.stop();
            } catch (Capture.CaptureException exception) {
                ExceptionHandler.handle(exception);
            }
        });
      
        root.getChildren().add(borderPane);
    }

    private void runComputationForSingleLine(boolean updateChart) throws IOException
    {
        compute.colorize(imageCanvas);
        double yborder = (compute.ymax - compute.ymin) * 0.1;
        if (updateChart) {
            yAxis.setLowerBound(compute.ymin - yborder);
            yAxis.setUpperBound(compute.ymax + yborder);
        }

        compute.chybicka = compute.chybickaHandler;
        compute.leftBorder = compute.maximumIgnoreBorderLeftHandler;
        compute.bottomBorder = compute.maximumIgnoreBorderBottomHandler;
        compute.analyze();

        // Redraw the graph
        if (updateChart) {
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

            textArea.clear();
        }
        calculateRs();
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

    private void loadCalibrationFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose the intrinsic file");
        fileChooser.setInitialDirectory(
                new File(System.getProperty("user.home"))
        );
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*")
        );
        File intrinsic = fileChooser.showOpenDialog(stage);
        fileChooser.setInitialDirectory(intrinsic.getParentFile());
        fileChooser.setTitle("Choose the distortion coefficients file");
        File dist = fileChooser.showOpenDialog(stage);
        calibration.loadCalibration(intrinsic.getAbsolutePath(), dist.getAbsolutePath());
    }

    /**
     * Launches the app.
     * @param args are the commandline arguments.
     */
    public static void main(String[] args) {
        launch();
    }
}