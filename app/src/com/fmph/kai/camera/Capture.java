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

public class Capture {
    private boolean capturing;
    private final VideoCapture capture;
    private ScheduledExecutorService timer;
    
    private OnFrameReceived onFrameReceived = frame -> {};

    public interface OnFrameReceived {
        void invoke(Mat frame);
    }

    public boolean isCapturing() {
        return capturing;
    }
    
    public void setOnFrameReceived(OnFrameReceived onFrameReceived) {
        this.onFrameReceived = onFrameReceived;
    }

    public Capture() {
        capturing = false;
        capture = new VideoCapture();
    }

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

    public static Image Mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }
    
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

    public static class CaptureException extends Exception {
        public CaptureException(String msg) {
            super(msg);
        }
    }
}
