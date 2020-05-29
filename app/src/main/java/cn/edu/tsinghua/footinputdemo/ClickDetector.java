package cn.edu.tsinghua.footinputdemo;

import org.opencv.core.Point;

import java.util.LinkedList;

public class ClickDetector {
    private LinkedList<GestureDetector.FootEllipse> ellipseTrace;
    private LinkedList<Point> pointTrace;
    public static final int UP_DOWN_FRAMES = 3;
    public static final double UP_DOWN_DIST = 10;

    public static final int HOLD_FRAMES = 10;
    public static final int TRACE_FRAMES = 60;
    public static final int BUFFER_FRAMES = 15;

    private static final double MOVING_AVERAGE_RATE = 0.8;
    private Point movingAvePoint;

    private Point currentPressingShape;
    private boolean isPressing, isClicking;
    private int frameCount, remainingClearFrame;
    private int downFrames, lastDownFrame; // Have been pressing down for downFrames frames
    private int upFrames, lastUpFrame; // Have been lifting up for upFrames frames
    private double downDist, upDist;

    public ClickDetector() {
        ellipseTrace = new LinkedList<>();
        pointTrace = new LinkedList<>();
        movingAvePoint = null;
        frameCount = 0;
        downFrames = upFrames = 0;
        lastDownFrame = lastUpFrame = -1;
        downDist = upDist = 0;
        remainingClearFrame = 0;
    }

    public void addState(GestureDetector.FootEllipse ellipse, Point fingerPoint) {
        if (remainingClearFrame > 0) {
            // ignore this frame
            remainingClearFrame -= 1;
            return;
        }

        ellipseTrace.addFirst(ellipse);
        if (ellipseTrace.size() > TRACE_FRAMES) {
            ellipseTrace.removeLast();
        }

        if (movingAvePoint == null) {
            // first point ever
            movingAvePoint = fingerPoint;
            pointTrace.addFirst(fingerPoint);
        } else {
            movingAvePoint = new Point(movingAvePoint.x * MOVING_AVERAGE_RATE + fingerPoint.x * (1-MOVING_AVERAGE_RATE),
                    movingAvePoint.y * MOVING_AVERAGE_RATE + fingerPoint.y * (1-MOVING_AVERAGE_RATE));
        }

        if (pointTrace.getFirst().y < fingerPoint.y) {
            downFrames += 1;
            downDist += fingerPoint.y - pointTrace.getFirst().y;
            if (downFrames >= UP_DOWN_FRAMES && downDist > UP_DOWN_DIST) {
                lastDownFrame = frameCount;
            }
            upFrames = 0;
            upDist = 0;
        } else {
            upFrames += 1;
            upDist += pointTrace.getFirst().y - fingerPoint.y;
            if (upFrames >= UP_DOWN_FRAMES && upDist > UP_DOWN_DIST) {
                lastUpFrame = frameCount;
            }
            downFrames = 0;
            downDist = 0;
        }

        pointTrace.addFirst(fingerPoint);
        if (pointTrace.size() > TRACE_FRAMES) {
            pointTrace.removeLast();
        }

        frameCount += 1;
    }

    public void clear() {
        downFrames = upFrames = 0;
        lastUpFrame = lastDownFrame = -1;
        downDist = upDist = 0;
        movingAvePoint = null;
        ellipseTrace.clear();
        pointTrace.clear();
    }

    public void clear(int clearFrames) {
        clear();
        remainingClearFrame = clearFrames;
    }

    public boolean isPressing() {
        // Pressing, if the foot has been kept down for HOLD_FRAMES frames, and haven't been lift back.
        return (lastUpFrame < lastDownFrame && lastDownFrame >= 0 && (frameCount - lastDownFrame > HOLD_FRAMES));
    }

    public boolean isClicking() {
        // Clicking, if the foot was pressed down and lift back within HOLD_FRAMES frames.
        // This signal will be kept for HOLD_FRAMES frames after that, unless clear() is called.
        return (lastUpFrame > 0 && lastDownFrame > 0 && (lastUpFrame - lastDownFrame < HOLD_FRAMES) && (frameCount - lastUpFrame < HOLD_FRAMES));
    }
}
