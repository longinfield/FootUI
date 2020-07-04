package cn.edu.tsinghua.footinputdemo;

import android.util.Log;

import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;

import java.util.ArrayList;

public class FootState {
    public class FootKeyPoint {
        public Point position;
        public Point smoothedPos;
        public PointMotionDetector motionDetector;
        public Size footSize;

        public FootKeyPoint() {
            this.position = null;
            this.motionDetector = new PointMotionDetector();
        }

        public void setFootSize(Size footSize) {
            this.footSize = footSize;
            this.motionDetector.setFootSize(footSize);
        }

        public void setPoint(long time, Point position) {
            smoothedPos = this.motionDetector.addPoint(time, position);
            this.position = position;
        }
    }

    public class FootDescriptor {
        public FootKeyPoint center;
        public FootKeyPoint finger;
        public FootKeyPoint bottom;
        public Rect boundingBox;
        public double angle;
        public double baseAngle;
        private static final int LEANING_ANGLE_THRESHOLD = 10;
        public static final int LEFT_BASE_ANGLE = 85;
        public static final int RIGHT_BASE_ANGLE = 95;

        public FootDescriptor(double baseAngle) {
            center = new FootKeyPoint();
            finger = new FootKeyPoint();
            bottom = new FootKeyPoint();
            this.baseAngle = baseAngle;
        }

        public void setFootSize(Size footSize) {
            center.setFootSize(footSize);
            finger.setFootSize(footSize);
            bottom.setFootSize(footSize);
        }

        public void setPoint(long time, Point center, Point finger, Point bottom) {
            this.center.setPoint(time, center);
            this.finger.setPoint(time, finger);
            this.bottom.setPoint(time, bottom);
        }

        public boolean isLeaningLeft() {
            Log.i("leaning", String.valueOf(angle) + " " + String.valueOf(finger.motionDetector.isMovingLeft) + " " + String.valueOf(finger.motionDetector.isMovingRight));
            return (angle > baseAngle + LEANING_ANGLE_THRESHOLD) && finger.motionDetector.isMovingLeft && !bottom.motionDetector.isMovingLeft;
        }

        public boolean isLeaningRight() {
            return (angle < baseAngle - LEANING_ANGLE_THRESHOLD) && finger.motionDetector.isMovingRight && !bottom.motionDetector.isMovingRight;
        }
    }

    public FootDescriptor left;
    public FootDescriptor right;
    public Size footSize;
    public boolean touched;
    private static final double DIST_FOOTSIZE_RATIO_THRESHOLD = 1;

    public FootState() {
        left = new FootDescriptor(FootDescriptor.LEFT_BASE_ANGLE);
        right = new FootDescriptor(FootDescriptor.RIGHT_BASE_ANGLE);
    }

    public void setFootSize(Size footSize) {
        this.footSize = footSize;
        left.setFootSize(footSize);
        right.setFootSize(footSize);
    }

    public void update(long time, Point[] left, Point[] right, boolean touched) {
        assert (left.length == 3 && right.length == 3);
        this.touched = touched;
        this.left.setPoint(time, left[0], left[1], left[2]);
        this.right.setPoint(time, right[0], right[1], right[2]);
    }

    public boolean isFeetNear() {
        return (left.boundingBox.x + left.boundingBox.width > right.boundingBox.x - footSize.width * 0.1);
    }

    public boolean isPointNear(Point l, Point r) {
        double distThreshold = footSize.width * DIST_FOOTSIZE_RATIO_THRESHOLD;
        return (l.x - r.x) * (l.x - r.x) + (l.y - r.y) * (l.y - r.y) < distThreshold * distThreshold;
    }

    public void clearClickDetection() {
        clearClickDetection(0);
    }

    public void clearClickDetection(int mills) {
        left.finger.motionDetector.clearClickDetection(mills);
        left.center.motionDetector.clearClickDetection(mills);
        left.bottom.motionDetector.clearClickDetection(mills);
        right.finger.motionDetector.clearClickDetection(mills);
        right.center.motionDetector.clearClickDetection(mills);
        right.bottom.motionDetector.clearClickDetection(mills);
    }

    public void  clearAllDetection(int mills) {
        left.finger.motionDetector.clearAllDetection(mills);
        left.center.motionDetector.clearAllDetection(mills);
        left.bottom.motionDetector.clearAllDetection(mills);
        right.finger.motionDetector.clearAllDetection(mills);
        right.center.motionDetector.clearAllDetection(mills);
        right.bottom.motionDetector.clearAllDetection(mills);
    }

}
