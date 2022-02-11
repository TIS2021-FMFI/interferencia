package com.fmph.kai.camera;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.opencv.videoio.Videoio.CAP_DSHOW;

/**
 * Handles the camera capturing process.
 */
public class Capture {
    private boolean capturing;
    private final VideoCapture capture;
    private ScheduledExecutorService timer;
    
    private OnFrameReceived onFrameReceived = frame -> {};

    /**
     * Allows to define the actions performed after a new frame from the camera was received.
     */
    public interface OnFrameReceived {
        void invoke(Mat frame);
    }

    /**
     * Tells if the capture is on or off.
     * @return true if capturing now, else false
     */
    public boolean isCapturing() {
        return capturing;
    }

    /**
     * Sets the callback function invoked after a frame from the camera was received.
     * @param onFrameReceived the callback function
     */
    public void setOnFrameReceived(OnFrameReceived onFrameReceived) {
        this.onFrameReceived = onFrameReceived;
    }

    /**
     * Initializes the OpenCV VideoCapture.
     */
    public Capture() {
        capturing = false;
        capture = new VideoCapture();
    }

    /**
     * Starts the capturing process with the camera index specified.
     * @param cameraIndex the index of the camera to start capture with
     * @throws CaptureException if the capture has been already started or failed to open the camera
     */
    public void start(int cameraIndex) throws CaptureException {
        if (capturing || capture.isOpened()) {
            throw new CaptureException("Already capturing.");
        }
        if (!capture.open(cameraIndex + CAP_DSHOW)) {
            throw new CaptureException("Error starting video using the camera index " + cameraIndex + ".");
        }
        capturing = true;
        Runnable frameGrabber = () -> {
            Mat frame = grabFrame();
            Platform.runLater(() -> {
                if (capturing && capture.isOpened()) {
                    onFrameReceived.invoke(frame);
                }
            });
        };
        timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the capturing.
     * @throws CaptureException if the capture hasn't been started
     */
    public void stop() throws CaptureException {
        if (!capturing || !capture.isOpened()) {
            throw new CaptureException("Capture is not opened.");
        }
        capture.release();
        capturing = false;
        timer.shutdown();
    }

    private Mat grabFrame() {
        if (capture.isOpened() && capturing) {
            Mat frame = new Mat();
            capture.read(frame);
            return frame;
        }
        return null;
    }

    /**
     * Converts the OpenCV matrix into the JavaFX Image.
     * @param frame the matrix
     * @return the converted image
     * @see Mat
     * @see Image
     */
    public static Image Mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    /**
     * Runs the Python script to get the cameras available on the PC.
     * @return the list with camera indexes
     * @throws CaptureException if the Python script has failed
     */
    public static ObservableList<Integer> getAvailableCameras() throws CaptureException {
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

            StringBuilder msg = new StringBuilder();
            while ((s = stdError.readLine()) != null) {
                msg.append(s);
            }
            if (!msg.toString().isEmpty()) {
                throw new CaptureException("Error running Python script: " + msg);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return cameraIndexes;
    }

    /**
     * Custom exception for this class only.
     */
    public static class CaptureException extends Exception {
        public CaptureException(String msg) {
            super(msg);
        }
    }
}
