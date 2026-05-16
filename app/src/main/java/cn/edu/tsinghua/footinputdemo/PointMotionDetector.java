package cn.edu.tsinghua.footinputdemo;

import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.video.KalmanFilter;

import java.util.Iterator;
import java.util.LinkedList;

public class PointMotionDetector {
    LinkedList<Point> pointTrace;
    LinkedList<Long> timeTrace;

    private static final int KEEP_FRAME_TIME_MS = 3000;
    private static final double MOVE_DIST_TO_FOOT_WIDTH_RATIO = 0.2; // detect moving for 0.2 * foot width
    private static final double MOVE_TIME_MS = 500; // detect moving in 500ms
    private double moveDistThreshold;

    // state descriptors
    public boolean isMovingUp;
    public boolean isMovingDown;
    public boolean isMovingLeft;
    public boolean isMovingRight;
    public boolean isStatic;

    // instant state descriptor (detect motion for 2 frames)
    public boolean isInstantlyMovingUp, isInstantlyMovingDown;
    private static final int INSTANT_MOTION_FRAMES = 2;

    // click state descriptors
    public boolean isClicking; // In case we have to recognize two feet clicking together, this signal should be preserved for a few milliseconds.
    public boolean isReadyToClick; // This happens when the finger is pressed down, but not for 500ms so it's not a long pressing event.
    public boolean isLongPressing;
    private long lastDownTime, lastDownStartTime, lastUpTime, lastUpStartTime;
    private long lastClickTime, clearClickEndTime, clearAllEndTime;
    private static final int LONG_PRESS_TIME_THRESHOLD_MS = 500;
    private static final int CLICK_EVENT_LAST_TIME = 300;

    private Size footSize;
    private KalmanFilter2D filter;

    public PointMotionDetector() {
        pointTrace = new LinkedList<>();
        timeTrace = new LinkedList<>();
        lastDownTime = lastUpTime = lastDownStartTime = lastUpStartTime = 0;
        lastClickTime = clearClickEndTime = clearAllEndTime = 0;
    }

    public void setFootSize(Size footSize) {
        this.footSize = footSize;
        moveDistThreshold = this.footSize.width * MOVE_DIST_TO_FOOT_WIDTH_RATIO;
    }

    public Point addPoint(long timestamp, Point point) {
        Point predPoint;
        if (filter == null) {
            filter = new KalmanFilter2D();
            filter.init(point);
            predPoint = point;
        } else {
            predPoint = filter.updateMeasurement(point);
        }

        // judge moving direction
        isMovingRight = isMovingLeft = isMovingDown = isMovingUp = false;
        isInstantlyMovingUp = isInstantlyMovingDown = false;
        isClicking = isLongPressing = isReadyToClick = false;

        Iterator<Point> pointIter = pointTrace.iterator();
        Iterator<Long> timeIter = timeTrace.iterator();
        boolean upDownMotionDetected = false, leftRightMotionDetected = false;
        int frameIndex = 0;
        for (; pointIter.hasNext(); ) {
            Point prevPoint = pointIter.next();
            Long prevTime = timeIter.next();
            if (prevTime < clearAllEndTime) break;
            if (frameIndex == INSTANT_MOTION_FRAMES) {
                if (prevPoint.y > predPoint.y) {
                    isInstantlyMovingUp = true;
                } else {
                    isInstantlyMovingDown = true;
                }
            }

            if (timestamp - prevTime < MOVE_TIME_MS ) {
                if (!upDownMotionDetected) {
                    if (predPoint.y - prevPoint.y > moveDistThreshold) {
                        upDownMotionDetected = true;
                        isMovingDown = true;
                        isMovingUp = false;
                        triggerDownEvent(timestamp, prevTime);
                    }

                    if (predPoint.y - prevPoint.y < -moveDistThreshold) {
                        upDownMotionDetected = true;
                        isMovingDown = false;
                        isMovingUp = true;
                        triggerUpEvent(timestamp, prevTime);
                    }
                }
                if (!leftRightMotionDetected) {
                    if (predPoint.x - prevPoint.x > moveDistThreshold) {
                        leftRightMotionDetected = true;
                        isMovingLeft = false;
                        isMovingRight = true;
                    }

                    if (predPoint.x - prevPoint.x < -moveDistThreshold) {
                        leftRightMotionDetected = true;
                        isMovingLeft = true;
                        isMovingRight = false;
                    }
                }
            }

            if (timestamp - prevTime > KEEP_FRAME_TIME_MS) {
                pointIter.remove();
                timeIter.remove();
            }
            frameIndex += 1;
        }

        isStatic = !upDownMotionDetected && !leftRightMotionDetected;
        isLongPressing = lastUpTime < lastDownTime && lastDownTime >= 0 && timestamp - lastDownTime > LONG_PRESS_TIME_THRESHOLD_MS;
        isReadyToClick = lastUpTime < lastDownTime && lastDownTime >= 0 && timestamp - lastDownTime < LONG_PRESS_TIME_THRESHOLD_MS;
        isClicking = timestamp - lastClickTime < CLICK_EVENT_LAST_TIME;

        pointTrace.addFirst(predPoint);
        timeTrace.addFirst(Long.valueOf(timestamp));
        return predPoint;
    }

    private void triggerDownEvent(long time, long prevTime) {
        if (prevTime >= lastDownStartTime && prevTime > clearClickEndTime) {
            lastDownStartTime = prevTime;
            lastDownTime = time;
        }
    }

    private void triggerUpEvent(long time, long prevTime) {
        if (prevTime >= lastUpStartTime) {
            lastUpStartTime = prevTime;
            lastUpTime = time;

            // click detection
            if (time > lastDownTime && lastDownTime > 0 && (time - lastDownTime < LONG_PRESS_TIME_THRESHOLD_MS)) {
                lastClickTime = time;
            }

            if (time > lastDownTime && lastDownTime > 0) {
                // if the finger point is moving up, then long pressing is cancelled, click event is triggered
                // therefore lastDownTime should be reset.
                lastDownTime = 0;
                lastDownStartTime = 0;
            }
        }
    }

    public void clearClickDetection(int mills) {
        lastDownTime = lastDownStartTime = 0;
        clearClickEndTime = timeTrace.getFirst() + mills;
    }

    public void clearAllDetection(int mills) {
        clearClickDetection(mills);
        lastUpTime = lastUpStartTime = 0;
        clearAllEndTime = timeTrace.getFirst() + mills;
    }
}
