package cn.edu.tsinghua.footinputdemo;

import android.util.Pair;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class GestureDetector implements FootState2Operation.FootOperationListener {

    public class FootEllipse {
        public Point center;
        public Point mainDir;
        public Size size;
        public Point finger;
        public Point bottom;
        public Rect boundingBox;

        public FootEllipse(Point center, Point mainDir, Size size) {
            this.center = center;
            this.mainDir = mainDir;
            this.size = size;
        }
    }

    private FootState2Operation operation;
    private FootEllipse prevEllipseL, prevEllipseR;
    private Mat tempFrame;
    private FootState footState;

    public GestureDetector() {
        prevEllipseL = prevEllipseR = null;
    }

    public void setFootOperationListener(FootState2Operation.FootOperationListener listener) {
        operation = new FootState2Operation(listener);
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

    private FootEllipse contourToFootEllipse(RotatedRect fit_ellipse, MatOfPoint2f contour, boolean isLeft) {
        Point center = fit_ellipse.center;
        Size size = fit_ellipse.size, footSize;
        double angle = fit_ellipse.angle;
        Point mainDir;
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
        FootEllipse footEllipse = new FootEllipse(center, mainDir, footSize);
        footEllipse = analyzeFingerPoint(footEllipse, contour, isLeft);

        footEllipse.boundingBox = Imgproc.boundingRect(contour);
        return footEllipse;
    }

    private FootEllipse analyzeFingerPoint(FootEllipse ellipse, MatOfPoint2f contour, boolean isLeft) {
        double maxDist = Double.NEGATIVE_INFINITY, minDist = Double.POSITIVE_INFINITY;
        Point fingerPoint = null, bottomPoint = null;
        List<Point> contourPoints = contour.toList();
        Point subDir;
        if (isLeft) {
            subDir = new Point(ellipse.mainDir.y, -ellipse.mainDir.x);
        } else {
            subDir = new Point(-ellipse.mainDir.y, ellipse.mainDir.x);
        }

        for (Iterator<Point> iter = contourPoints.iterator(); iter.hasNext(); ) {
            Point p = iter.next();
            Point diff = new Point(p.x - ellipse.center.x, p.y - ellipse.center.y);
            double dist = ellipse.mainDir.dot(diff);
            if (dist > maxDist) {
                maxDist = dist;
                fingerPoint = p;
            }

            double distBottom = ellipse.mainDir.dot(diff) + subDir.dot(diff);
            if (distBottom < minDist) {
                minDist = distBottom;
                bottomPoint = p;
            }
        }

        ellipse.finger = fingerPoint;
        ellipse.bottom = bottomPoint;
        return ellipse;
    }

    private void updateFootState(FootEllipse ellipseL, FootEllipse ellipseR, boolean touched) {
        if (footState == null) {
            footState = new FootState();
            footState.setFootSize(new Size((ellipseL.size.width + ellipseR.size.width) / 2,
                    (ellipseL.size.height + ellipseR.size.height) / 2));
        }
        footState.left.angle = Math.toDegrees(Math.atan2(-ellipseL.mainDir.y, ellipseL.mainDir.x));
        footState.right.angle = Math.toDegrees(Math.atan2(-ellipseR.mainDir.y, ellipseR.mainDir.x));
        footState.left.boundingBox = ellipseL.boundingBox;
        footState.right.boundingBox = ellipseR.boundingBox;

        Point[] leftPoints = {ellipseL.center, ellipseL.finger, ellipseL.bottom};
        Point[] rightPoints = {ellipseR.center, ellipseR.finger, ellipseR.bottom};
        footState.update(System.currentTimeMillis(), leftPoints, rightPoints, touched);
    }

    private void renderFrame(Mat frame, MatOfPoint2f cont_l, MatOfPoint2f cont_r, RotatedRect ellipse_l, RotatedRect ellipse_r, Point finger_point_l, Point finger_point_r, FootState state) {
        List<MatOfPoint> cont = new ArrayList<>(Arrays.asList(new MatOfPoint(cont_l.toArray()),  new MatOfPoint(cont_r.toArray())));
        Imgproc.drawContours(frame, cont, -1, new Scalar(0, 127, 0));
        Imgproc.ellipse(frame, ellipse_l, new Scalar(0, 0, 255));
        Imgproc.ellipse(frame, ellipse_r, new Scalar(0, 0, 255));
        Imgproc.circle(frame, finger_point_l, 10, new Scalar(255, 0, 0));
        Imgproc.circle(frame, finger_point_r, 10, new Scalar(255, 0, 0));
        Imgproc.circle(frame, state.left.finger.smoothedPos, 10, new Scalar(255, 255, 255));
        Imgproc.circle(frame, state.right.finger.smoothedPos, 10, new Scalar(255, 255, 255));
        Imgproc.circle(frame, state.left.bottom.smoothedPos, 10, new Scalar(255, 255, 255));
        Imgproc.circle(frame, state.right.bottom.smoothedPos, 10, new Scalar(255, 255, 255));
        Imgproc.circle(frame, state.left.center.smoothedPos, 10, new Scalar(255, 255, 255));
        Imgproc.circle(frame, state.right.center.smoothedPos, 10, new Scalar(255, 255, 255));
        tempFrame = frame;
//        writeTextToTempFrame(String.valueOf(state.left.angle) + " " + String.valueOf(state.right.angle), 2);
        operation.checkFootState(state);
    }

    public Mat processFrame(Mat frame, List<Pair<Double, MatOfPoint>> contours) {
        RotatedRect rect_l, rect_r;
        MatOfPoint2f cont_l, cont_r;
        boolean touched = false;
        if (contours.size() == 2) {
            MatOfPoint2f cont0 = new MatOfPoint2f(contours.get(0).second.toArray());
            MatOfPoint2f cont1 = new MatOfPoint2f(contours.get(1).second.toArray());
            RotatedRect rect0 = Imgproc.fitEllipse(cont0);
            RotatedRect rect1 = Imgproc.fitEllipse(cont1);

            if (rect0.center.x < rect1.center.x) {
                cont_l = cont0; cont_r = cont1; rect_l = rect0; rect_r = rect1;
            } else {
                cont_l = cont1; cont_r = cont0; rect_l = rect1; rect_r = rect0;
            }
        } else {
            if (contours.size() == 1) {
                MatOfPoint2f contour = new MatOfPoint2f(contours.get(0).second.toArray());
                List<Point> cont_l_list = new ArrayList<>(), cont_r_list = new ArrayList<>();
                if (splitContours(contour, cont_l_list, cont_r_list)) {
                    cont_l = new MatOfPoint2f();
                    cont_r = new MatOfPoint2f();
                    cont_l.fromList(cont_l_list);
                    cont_r.fromList(cont_r_list);
                    rect_l = Imgproc.fitEllipse(cont_l);
                    rect_r = Imgproc.fitEllipse(cont_r);
                    touched = true;
                } else {
                    prevEllipseL = prevEllipseR = null;
                    return frame;
                }
            } else {
                prevEllipseL = prevEllipseR = null;
                return frame;
            }
        }

        FootEllipse ellipse_l = contourToFootEllipse(rect_l, cont_l, true);
        FootEllipse ellipse_r = contourToFootEllipse(rect_r, cont_r, false);
        prevEllipseL = ellipse_l; prevEllipseR = ellipse_r;

        updateFootState(ellipse_l, ellipse_r, touched);
        renderFrame(frame, cont_l, cont_r, rect_l, rect_r, footState.left.finger.position, footState.right.finger.position, footState);
        return frame;
    }

    private void writeTextToTempFrame(String text) {
        writeTextToTempFrame(text, 0);
    }

    private void writeTextToTempFrame(String text, int row) {
        Imgproc.putText(tempFrame, text, new Point(20, 40 * (row+1)), Imgproc.FONT_HERSHEY_COMPLEX, 1, new Scalar(0, 255, 0), 2);
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
    }

    @Override
    public void onDrag(Point dragPointer) {
        writeTextToTempFrame("Drag: " + dragPointer.toString());
        Imgproc.circle(tempFrame, dragPointer, 20, new Scalar(255, 0, 0), 5);
    }

    @Override
    public void onEasyAccess() {
        writeTextToTempFrame("Easy Access");
    }

    @Override
    public void onFlipScreenLeft() {
        writeTextToTempFrame("Flip Screen Left");
    }

    @Override
    public void onFlipScreenRight() {
        writeTextToTempFrame("Flip Screen Right");
    }

    @Override
    public void onHome() {
        writeTextToTempFrame("Home");
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
    }

    @Override
    public void onShortCutMenu() {
        writeTextToTempFrame("Short Cut Menu");
    }

    @Override
    public void onTrigger(Point pointer) {
        writeTextToTempFrame("Trigger: " + pointer.toString());
        Imgproc.circle(tempFrame, pointer, 20, new Scalar(255, 0, 0), 5);
    }

    @Override
    public void onVolume() {
        writeTextToTempFrame("Volume");
    }
}
