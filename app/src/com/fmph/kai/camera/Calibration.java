package com.fmph.kai.camera;

import com.fmph.kai.util.ExceptionHandler;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Calibration {
    private final Mat intrinsic = new Mat(3, 3, CvType.CV_32FC1);
    private final Mat distCoeffs = new Mat();
    private final MatOfPoint3f obj = new MatOfPoint3f();
    private final List<Mat> imagePoints = new ArrayList<>();
    private final List<Mat> objectPoints = new ArrayList<>();
    private MatOfPoint2f imageCorners = new MatOfPoint2f();
    private boolean calibrated = false;
    private int numSnapshots = 0;
    private final int numRequired ;
    private Size size;
    private OnCalibrated onCalibrated;

    public Calibration(Size size, int numRequiredSnapshots) {
        this.size = size;
        this.numRequired = numRequiredSnapshots;
        for (double j = 0; j < size.width * size.height; j++) {
            obj.push_back(new MatOfPoint3f(new Point3(j / size.width, j % size.height, 0.0f)));
        }
    }

    public boolean isCalibrated() {
        return calibrated;
    }

    public int getNumSnapshots() {
        return numSnapshots;
    }

    public void setOnCalibrated(OnCalibrated onCalibrated) {
        this.onCalibrated = onCalibrated;
    }

    public Mat calibrateImage(Mat frame) {
        Mat undistorted = new Mat();
        Calib3d.undistort(frame, undistorted, intrinsic, distCoeffs);
        return undistorted;
    }

    private void saveCalibration() {
        saveDoubleMat(intrinsic, "intrinsic");
        saveDoubleMat(distCoeffs, "distortion");
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

    public void loadDoubleMat(final Mat mat, final String fileName) {
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

    public boolean findAndDrawPoints(Mat frame) {
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        boolean found = Calib3d.findChessboardCorners(gray, size, imageCorners, Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK);
        if (found) {
            TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 30, 0.1);
            Imgproc.cornerSubPix(gray, imageCorners, new Size(11, 11), new Size(-1, -1), term);
            size = gray.size();
            Calib3d.drawChessboardCorners(frame, size, imageCorners, true);
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

        saveCalibration();
        onCalibrated.invoke();
    }

    public void newSnapshot() {
        numSnapshots++;
        imagePoints.add(imageCorners.clone());
        imageCorners = new MatOfPoint2f();
        objectPoints.add(obj.clone());
        if (numSnapshots == numRequired) {
            calibrate();
        }
    }

    public interface OnCalibrated {
        void invoke();
    }
}
