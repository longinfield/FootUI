package cn.edu.tsinghua.footinputdemo;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class GestureDetector implements FootState2Operation.FootOperationListener {

    public class FootEllipse {
        public Point center;
        public Point mainDir;
        public Size size;

        public FootEllipse(Point center, Point mainDir, Size size) {
            this.center = center;
            this.mainDir = mainDir;
            this.size = size;
        }
    }

    private FootState2Operation operation;
    private FootEllipse prevEllipseL, prevEllipseR;
    private Point prevPointL, prevPointR;
    private Mat tempFrame;
    private PointerMotionDetector motionDetectorL, motionDetectorR;
    private ClickDetector clickDetectorL, clickDetectorR;

    public GestureDetector() {
        prevEllipseL = prevEllipseR = null;
        operation = new FootState2Operation(this);
        motionDetectorL = new PointerMotionDetector();
        motionDetectorR = new PointerMotionDetector();
        clickDetectorL = new ClickDetector();
        clickDetectorR = new ClickDetector();
    }

    private double distToEllipse(Point p, FootEllipse rect) {
        Point diff = new Point(p.x - rect.center.x, p.y - rect.center.y);
        Point subDir = new Point(rect.mainDir.y, -rect.mainDir.x);
        double mainDist = diff.dot(rect.mainDir) / rect.size.height;
        double subDist = diff.dot(subDir) / rect.size.width;
        return mainDist * mainDist + subDist * subDist;
    }

    private boolean splitContours(MatOfPoint2f contour, List<Point> cont_l, List<Point> cont_r) {
        if (prevEllipseL == null || prevEllipseR == null) {
            return false;
        }
        List<Point> contourList = contour.toList();
        for (Iterator<Point> iter = contourList.iterator(); iter.hasNext();) {
            Point p = iter.next();
            double dist_l = distToEllipse(p, prevEllipseL);
            double dist_r = distToEllipse(p, prevEllipseR);
            if (dist_l < dist_r) {
                cont_l.add(p);
            } else {
                cont_r.add(p);
            }
        }
        return true;
    }

    private FootEllipse contourToFootEllipse(RotatedRect fit_ellipse) {
        Point center = fit_ellipse.center;
        Size size = fit_ellipse.size, footSize = null;
        double angle = fit_ellipse.angle;
        Point mainDir = null;
        if (size.width > size.height) {
            angle = Math.toRadians(angle);
            mainDir = new Point(Math.cos(angle), -Math.sin(angle));
            footSize = new Size(size.height, size.width);
        } else {
            if (angle > 90) angle -= 180;
            angle = Math.toRadians(angle);
            mainDir = new Point(Math.sin(angle), -Math.cos(angle));
            footSize = size;
        }

        return new FootEllipse(center, mainDir, footSize);
    }

    private Point analyzeFingerPoint(FootEllipse ellipse, MatOfPoint2f contour) {
        double maxDist = Double.NEGATIVE_INFINITY;
        Point fingerPoint = null;
        List<Point> contourPoints = contour.toList();
        for (Iterator<Point> iter = contourPoints.iterator(); iter.hasNext(); ) {
            Point p = iter.next();
            Point diff = new Point(p.x - ellipse.center.x, p.y - ellipse.center.y);
            double dist = ellipse.mainDir.dot(diff);
            if (dist > maxDist) {
                maxDist = dist;
                fingerPoint = p;
            }
        }
        return fingerPoint;
    }

    private FootState footEllipse2FootState(FootEllipse ellipseL, FootEllipse ellipseR, Point fingerPointL, Point fingerPointR, boolean feet_touched) {
        FootState state = new FootState();
        state.leftPoint = ellipseL.center;
        state.rightPoint = ellipseR.center;

        if (feet_touched) {
            state.both |= FootState.IS_TOUCHING;
        }
        if (prevPointL != null && state.leftPoint.x > prevPointL.x && state.rightPoint.x < prevPointR.x) {
            state.both |= FootState.IS_APPROACHING;
        }
        if (prevPointL != null && state.leftPoint.x < prevPointL.x && state.rightPoint.x > prevPointR.x) {
            state.both |= FootState.IS_SEPARATING;
        }

        // 1. detect leaning left & right
        double angle_l = Math.toDegrees(Math.atan2(-ellipseL.mainDir.y, ellipseL.mainDir.x));
        if (angle_l > 100) {
            state.left |= FootState.IS_LEANING_LEFT;
        }
        if (angle_l < 70){
            state.left |= FootState.IS_LEANING_RIGHT;
        }

        double angle_r = Math.toDegrees(Math.atan2(-ellipseR.mainDir.y, ellipseR.mainDir.x));
        if (angle_r > 110) {
            state.right |= FootState.IS_LEANING_LEFT;
        }
        if (angle_r < 80){
            state.right |= FootState.IS_LEANING_RIGHT;
        }

        // 2. detect lifting / static
        motionDetectorL.addPoint(state.leftPoint);
        motionDetectorR.addPoint(state.rightPoint);
        if (motionDetectorL.isLifting())
            state.left |= FootState.IS_LIFTING;
        if (motionDetectorL.isStatic())
            state.left |= FootState.IS_STATIC;
        if (motionDetectorR.isLifting())
            state.right |= FootState.IS_LIFTING;
        if (motionDetectorR.isStatic())
            state.right |= FootState.IS_STATIC;

        // 3. detect pressing / clicking
        clickDetectorL.addState(ellipseL, fingerPointL);
        clickDetectorR.addState(ellipseR, fingerPointR);
        if (clickDetectorL.isPressing())
            state.left |= FootState.IS_PRESSING;
        if (clickDetectorL.isClicking())
            state.left |= FootState.IS_CLICKING;
        if (clickDetectorR.isPressing())
            state.right |= FootState.IS_PRESSING;
        if (clickDetectorR.isClicking())
            state.right |= FootState.IS_CLICKING;

        return state;
    }

    private void renderFrame(Mat frame, MatOfPoint2f cont_l, MatOfPoint2f cont_r, RotatedRect ellipse_l, RotatedRect ellipse_r, Point finger_point_l, Point finger_point_r, FootState state) {
        List<MatOfPoint> cont = new ArrayList<>(Arrays.asList(new MatOfPoint(cont_l.toArray()),  new MatOfPoint(cont_r.toArray())));
        Imgproc.drawContours(frame, cont, -1, new Scalar(0, 127, 0));
        Imgproc.ellipse(frame, ellipse_l, new Scalar(0, 0, 255));
        Imgproc.ellipse(frame, ellipse_r, new Scalar(0, 0, 255));
        Imgproc.circle(frame, finger_point_l, 5, new Scalar(255, 0, 0));
        Imgproc.circle(frame, finger_point_r, 5, new Scalar(255, 0, 0));
        tempFrame = frame;
        operation.checkFootState(state, finger_point_l, finger_point_r);
//        Imgproc.putText(frame, "l: " + pose_l + ", r: " + pose_r, new Point(20, 40), Imgproc.FONT_HERSHEY_COMPLEX, 1, new Scalar(0, 255, 0), 2);
    }

    public Mat processFrame(Mat frame, List<MatOfPoint> contours) {
        RotatedRect rect_l, rect_r;
        MatOfPoint2f cont_l, cont_r;
        boolean feet_touched = false;
        if (contours.size() == 2) {
            MatOfPoint2f cont0 = new MatOfPoint2f(contours.get(0).toArray());
            MatOfPoint2f cont1 = new MatOfPoint2f(contours.get(1).toArray());
            RotatedRect rect0 = Imgproc.fitEllipse(cont0);
            RotatedRect rect1 = Imgproc.fitEllipse(cont1);

            if (rect0.center.x < rect1.center.x) {
                cont_l = cont0; cont_r = cont1; rect_l = rect0; rect_r = rect1;
            } else {
                cont_l = cont1; cont_r = cont0; rect_l = rect1; rect_r = rect0;
            }
        } else {
            if (contours.size() == 1) {
                MatOfPoint2f contour = new MatOfPoint2f(contours.get(0).toArray());
                List<Point> cont_l_list = new ArrayList<>(), cont_r_list = new ArrayList<>();
                if (splitContours(contour, cont_l_list, cont_r_list)) {
                    cont_l = new MatOfPoint2f();
                    cont_r = new MatOfPoint2f();
                    cont_l.fromList(cont_l_list);
                    cont_r.fromList(cont_r_list);
                    rect_l = Imgproc.fitEllipse(cont_l);
                    rect_r = Imgproc.fitEllipse(cont_r);
                    feet_touched = true;
                } else {
                    prevEllipseL = prevEllipseR = null;
                    return frame;
                }
            } else {
                prevEllipseL = prevEllipseR = null;
                return frame;
            }
        }

        FootEllipse ellipse_l = contourToFootEllipse(rect_l);
        FootEllipse ellipse_r = contourToFootEllipse(rect_r);
        prevEllipseL = ellipse_l; prevEllipseR = ellipse_r;
        Point finger_point_l = analyzeFingerPoint(ellipse_l, cont_l);
        Point finger_point_r = analyzeFingerPoint(ellipse_r, cont_r);

        FootState footState = footEllipse2FootState(ellipse_l, ellipse_r, finger_point_l, finger_point_r, feet_touched);

        renderFrame(frame, cont_l, cont_r, rect_l, rect_r, finger_point_l, finger_point_r, footState);
        prevPointL = rect_l.center;
        prevPointR = rect_r.center;
        return frame;
    }

    private void writeTextToTempFrame(String text) {
        Imgproc.putText(tempFrame, text, new Point(20, 40), Imgproc.FONT_HERSHEY_COMPLEX, 1, new Scalar(0, 255, 0), 2);
    }

    private void clearClickDetector() {
        clickDetectorL.clear(ClickDetector.BUFFER_FRAMES);
        clickDetectorR.clear(ClickDetector.BUFFER_FRAMES);
    }

    @Override
    public void onLongTouch(Point point) {
        writeTextToTempFrame("Long Touch: " + point.toString());
        Imgproc.circle(tempFrame, point, 20, new Scalar(255, 0, 0), 5);
    }

    @Override
    public void onAppSwitch() {
        writeTextToTempFrame("App Switch");

    }

    @Override
    public void onBack() {
        writeTextToTempFrame("Back");
        clearClickDetector();
    }

    @Override
    public void onDrag(Point dragPointer) {
        writeTextToTempFrame("Drag: " + dragPointer.toString());
        Imgproc.circle(tempFrame, dragPointer, 20, new Scalar(255, 0, 0), 5);
    }

    @Override
    public void onEasyAccess() {
        writeTextToTempFrame("Easy Access");
        clearClickDetector();
    }

    @Override
    public void onFlipScreenLeft() {
        writeTextToTempFrame("Flip Screen Left");
        clearClickDetector();
    }

    @Override
    public void onFlipScreenRight() {
        writeTextToTempFrame("Flip Screen Right");
        clearClickDetector();
    }

    @Override
    public void onHome() {
        writeTextToTempFrame("Home");
        clearClickDetector();
    }

    @Override
    public void onNotice() {
        writeTextToTempFrame("Notice");
    }

    @Override
    public void onPointing(Point pointer) {
        writeTextToTempFrame("Pointing: " + pointer.toString());
        Imgproc.circle(tempFrame, pointer, 20, new Scalar(255, 0, 0), 5);
    }

    @Override
    public void onScreenshot() {
        writeTextToTempFrame("Screen Shot");
        clearClickDetector();
    }

    @Override
    public void onShortCutMenu() {
        writeTextToTempFrame("Short Cut Menu");
        clearClickDetector();
    }

    @Override
    public void onTrigger(Point pointer) {
        writeTextToTempFrame("Trigger: " + pointer.toString());
        Imgproc.circle(tempFrame, pointer, 20, new Scalar(255, 0, 0), 5);
    }

    @Override
    public void onVolume() {
        writeTextToTempFrame("Volume");
        clearClickDetector();
    }
}
