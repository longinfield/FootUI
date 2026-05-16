package cn.edu.tsinghua.footinputdemo;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

public class KalmanFilter2D {
    // Notation from: Position Estimation and Smooth Tracking With a Fuzzy-Logic-Based Adaptive Strong Tracking Kalman Filter for Capacitive Touch Panels
    private Mat A, C, Q, R;
    private static final double alpha = 0.5; // velocity factor
    private static final double q_var = 5, r_var = 5; // noise variance
    private Mat curX, predX, curP, predP;

    public KalmanFilter2D() {
        A = Mat.eye(4, 4, CvType.CV_32F);
        A.put(0, 1, alpha);
        A.put(2, 3, alpha);
        C = Mat.zeros(2, 4, CvType.CV_32F);
        C.put(0, 0, 1);
        C.put(1, 2, 1);
        Q = Mat.ones(4, 4, CvType.CV_32F);
        for (int i = 0; i < 4; i++) Q.put(i, i, q_var);
        R = Mat.ones(2, 2, CvType.CV_32F);
        for (int i = 0; i < 2; i++) R.put(i, i, r_var);
    }

    public void init(Point initPoint) {
        curX = Mat.zeros(4, 1, CvType.CV_32F);
        curX.put(0, 0, initPoint.x);
        curX.put(2, 0, initPoint.y);
        curP = Mat.eye(4, 4, CvType.CV_32F);
        predict();
    }

    private Mat matmul(Mat mat1, Mat mat2) {
        Mat res = new Mat(), placeholder = Mat.zeros(new Size(mat1.rows(), mat2.cols()), CvType.CV_32F);
        Core.gemm(mat1, mat2, 1, placeholder, 0, res);
        return res;
    }

    private void predict() {
        predX = matmul(A, curX);
        predP = matmul(matmul(A, curP), A.t());
        Core.add(predP, Q, predP);
//        Log.i("kalmanf", "pred: " + predX.toString());
    }

    public Point updateMeasurement(Point measurement) {
        Mat K = new Mat();
        Core.add(R, matmul(matmul(C, predP), C.t()), K);
        K = matmul(matmul(predP, C.t()), K.inv());

        Mat measureZ = Mat.zeros(2, 1, CvType.CV_32F);
        measureZ.put(0, 0, measurement.x);
        measureZ.put(1, 0, measurement.y);
        Mat diffX = new Mat();
        Core.subtract(measureZ, matmul(C, predX), diffX);
        Core.add(predX, matmul(K, diffX), curX);

        Core.subtract(Mat.eye(4, 4, CvType.CV_32F), matmul(K, C), curP);
        curP = matmul(curP, predP);
        Mat curZ = matmul(C, curX);

        // predict ahead for the next frame
        predict();
        return new Point(curZ.get(0, 0)[0], curZ.get(1, 0)[0]);
    }
}
