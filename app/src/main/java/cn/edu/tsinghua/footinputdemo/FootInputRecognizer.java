package cn.edu.tsinghua.footinputdemo;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class FootInputRecognizer {
    private int width, height;
    private SkinColorRecognizer skinColorRecognizer;
    private GestureDetector gestureDetector;
    private boolean need_calibration;
    private Mat prevFrame;

    // int[4] = top, bottom, left, right
    private int[] detect_area;
    private int[] calib_area_l;
    private int[] calib_area_r;
    private int necessaryContourArea;
    public SideBarService msideBarService;

    public FootInputRecognizer(SideBarService sideBarService, int screenWidth, int screenHeight,int screenDensity, WindowManager windowManager, Context context) {
        msideBarService=sideBarService;
        skinColorRecognizer = new SkinColorRecognizer();
        gestureDetector = new GestureDetector(sideBarService,screenWidth,screenHeight,screenDensity,windowManager,context);
        gestureDetector.setFootOperationListener(gestureDetector);
        need_calibration = true;

    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;

        // TODO: adjust magic numbers according to actual use
        detect_area = new int[] {height / 4, height * 3 / 4, width / 8, width * 7 / 8};
        calib_area_l = new int[] {height / 2 - 20, height / 2 + 20, width / 3 - 10, width / 3 + 10};
        calib_area_r = new int[] {height / 2 - 20, height / 2 + 20, width * 2 / 3 - 10, width * 2 / 3 + 10};
        necessaryContourArea = 3000;
    }

    private Mat calibrationNotation(Mat frame) {
        Imgproc.rectangle(frame, new Point(detect_area[2], detect_area[0]), new Point(detect_area[3], detect_area[1]), new Scalar(0, 255, 0));
        Imgproc.rectangle(frame, new Point(calib_area_l[2], calib_area_l[0]), new Point(calib_area_l[3], calib_area_l[1]), new Scalar(0, 127, 0));
        Imgproc.rectangle(frame, new Point(calib_area_r[2], calib_area_r[0]), new Point(calib_area_r[3], calib_area_r[1]), new Scalar(0, 127, 0));
        return frame;
    }

    private List<Pair<Double, MatOfPoint>> generateContours(Mat frame) {
        Imgproc.GaussianBlur(frame, frame, new Size(5,5), 0, 0);
        Mat mask = skinColorRecognizer.maskFrame(frame);
        Mat kernel = Mat.ones(5,5, CvType.CV_8U);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);


        List<Pair<Double, MatOfPoint>> validContours = new ArrayList<>();
        for (Iterator<MatOfPoint> i = contours.iterator(); i.hasNext(); ) {
            MatOfPoint contour = i.next();
            Double area = Imgproc.contourArea(contour);
            if (area > necessaryContourArea) {
                validContours.add(new Pair<>(area, contour));
            }
        }

        if (validContours.size() > 2) {
            Collections.sort(validContours, new Comparator<Pair<Double, MatOfPoint>>() {
                @Override
                public int compare(Pair<Double, MatOfPoint> o1, Pair<Double, MatOfPoint> o2) {
                    Double areaDiff = o1.first - o2.first;
                    if (areaDiff < -0.00001) {
                        return 1;
                    } else if (areaDiff > 0.00001) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            });

            validContours = validContours.subList(0, 2);
        }

        return validContours;
    }

    synchronized public Mat onCameraFrame(Mat frame) {
        prevFrame = frame.clone();
        if (need_calibration) {
            return calibrationNotation(frame);
        }

        Mat frameSubMat = frame.submat(detect_area[0], detect_area[1], detect_area[2], detect_area[3]);
        Mat roi = frameSubMat.clone();
        List<Pair<Double, MatOfPoint>> contours = generateContours(roi);
        roi = gestureDetector.processFrame(roi, contours);
        roi.copyTo(frameSubMat);
        return frame;
    }

    synchronized public void onClickInput() {
        Log.i("footdemo", "onClickInput");

        if (need_calibration) {
            // send cached frame to skinColorRecognizer as calibration reference
            Imgproc.GaussianBlur(prevFrame, prevFrame, new Size(5, 5), 0, 0);
            Mat roi_l = prevFrame.submat(calib_area_l[0], calib_area_l[1], calib_area_l[2], calib_area_l[3]).clone();
            Mat roi_r = prevFrame.submat(calib_area_r[0], calib_area_r[1], calib_area_r[2], calib_area_r[3]).clone();
            Mat roi = new Mat();
            Core.hconcat(Arrays.asList(roi_l, roi_r), roi);
            skinColorRecognizer.setCalibrationImage(roi);
            need_calibration = skinColorRecognizer.needCalibration();
        }
    }
}
