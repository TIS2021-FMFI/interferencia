package com.fmph.kai.camera;

import com.fmph.kai.util.ExceptionHandler;
import javafx.application.Platform;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The class, which handles the whole image calibration process, using the methods provided by the OpenCV library.
 */
public class Calibration {
    private final Mat intrinsic = new Mat(3, 3, CvType.CV_32FC1);
    private final Mat distCoeffs;
    private final MatOfPoint3f obj = new MatOfPoint3f();
    private final List<Mat> imagePoints = new ArrayList<>();
    private final List<Mat> objectPoints = new ArrayList<>();
    private MatOfPoint2f imageCorners = new MatOfPoint2f();
    private boolean calibrated = false;
    private int numSnapshots = 0;
    private final int numRequired;
    private Size size;
    private Size boardSize;
    private OnCalibrated onCalibrated;

    /**
     * Creates the Calibration instance with the specified size and the number of required snapshots.
     * Creates the `obj` matrix, which is the 3d representation of the board.
     * @param numRequiredSnapshots the number of the snapshots to be taken for the calibration
     */
    public Calibration(Size boardSize, int numRequiredSnapshots) {
        this.numRequired = numRequiredSnapshots;
        this.boardSize = boardSize;
        distCoeffs = new Mat();
        for (double j = 0; j < boardSize.width * boardSize.height; j++) {
            obj.push_back(new MatOfPoint3f(new Point3(j / boardSize.width, j % boardSize.height, 0.0f)));
        }
    }

    /**
     * Creates an empty calibration instance.
     * This constructor should be used only when the calibration is going to be loaded afterwards.
     */
    public Calibration() {
        numRequired = 0;
        distCoeffs = new Mat(1, 5, CvType.CV_64FC1);
    }

    /**
     * Tells if this calibration instance is calibrated (either by actual calibration process or by loading the calibration files.
     * @return true if calibrated and false if not
     */
    public boolean isCalibrated() {
        return calibrated;
    }

    /**
     * Returns the number of snapshots to be taken before the calibration itself occurs.
     * @return the number of snapshots
     */
    public int getNumSnapshots() {
        return numSnapshots;
    }

    /**
     * Sets the callback function, which is activated when the calibration process is finished.
     * @param onCalibrated the callback function
     */
    public void setOnCalibrated(OnCalibrated onCalibrated) {
        this.onCalibrated = onCalibrated;
    }

    /**
     * Calibrates the input image using the matrices storing the calibration data.
     * @param frame the matrix representing a frame
     * @return the matrix calibrated using intrinsic and extrinsic matrices
     */
    public Mat calibrateImage(Mat frame) {
        Mat undistorted = new Mat();
        Calib3d.undistort(frame, undistorted, intrinsic, distCoeffs);
        return undistorted;
    }


    public void saveCalibration(String directory) {
        saveDoubleMat(intrinsic, directory + "/intrinsic");
        saveDoubleMat(distCoeffs, directory + "/distortion");
    }

    /**
     * Loads the calibration matrices from the filesystem, which were created by `saveCalibration()`.
     * @param intrinsicFile is the path to the file containing the intrinsic matrix
     * @param distFile is the path to the file containing the extrinsic matrix
     */
    public void loadCalibration(String intrinsicFile, String distFile) {
        loadDoubleMat(intrinsic, intrinsicFile);
        loadDoubleMat(distCoeffs, distFile);
        calibrated = true;
    }

    private void saveDoubleMat(final Mat mat, final String fileName) {
        final long count = mat.total() * mat.channels();
        final double[] buff = new double[(int) count];
        mat.get(0, 0, buff);
        try (final DataOutputStream out = new DataOutputStream(new FileOutputStream(fileName))) {
            for (double v : buff) {
                out.writeDouble(v);
            }
        } catch (IOException e) {
            ExceptionHandler.handle(e);
        }
    }

    private void loadDoubleMat(final Mat mat, final String fileName) {
        final long count = mat.total() * mat.channels();
        final List<Double> list = new ArrayList<>();
        try (final DataInputStream inStream = new DataInputStream(new FileInputStream(fileName))) {
            for (int i = 0; i < count; ++i) {
                list.add(inStream.readDouble());
            }
        } catch (IOException e) {
            ExceptionHandler.handle(e);
        }
        final double[] buff = new double[list.size()];
        for (int i = 0; i < buff.length; i++) {
            buff[i] = list.get(i);
        }
        mat.put(0, 0, buff);
    }

    /**
     * Finds and draws the points on the corners of the inner chessboard squares if they were found.
     * @param frame the matrix representing an image, where corners will be drawn
     * @return true if the chessboard pattern was recognised and false if not
     */
    public boolean findAndDrawPoints(Mat frame) {
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        boolean found = Calib3d.findChessboardCorners(gray, boardSize, imageCorners, Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK);
        if (found) {
            TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 30, 0.1);
            try {
                Imgproc.cornerSubPix(gray, imageCorners, new Size(11, 11), new Size(-1, -1), term);
            } catch (CvException e) {
                return false;
            }
            size = gray.size();
            Calib3d.drawChessboardCorners(frame, boardSize, imageCorners, true);
            return true;
        }
        return false;
    }

    private void calibrate() {
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();
        intrinsic.put(0, 0, 1);
        intrinsic.put(1, 1, 1);
        Calib3d.calibrateCamera(objectPoints, imagePoints, size, intrinsic, distCoeffs, rvecs, tvecs);
        calibrated = true;

        onCalibrated.invoke();
    }

    /**
     * Updates the calibration matrices with the last point coordinates found.
     */
    public void newSnapshot() {
        numSnapshots++;
        imagePoints.add(imageCorners.clone());
        imageCorners = new MatOfPoint2f();
        objectPoints.add(obj.clone());
        if (numSnapshots == numRequired) {
            calibrate();
        }
    }

    /**
     * This is the interface for the callback function called after the calibration has been finished.
     */
    public interface OnCalibrated {
        void invoke();
    }
}
