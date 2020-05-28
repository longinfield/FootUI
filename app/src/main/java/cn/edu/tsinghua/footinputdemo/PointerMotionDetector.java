package cn.edu.tsinghua.footinputdemo;

import org.opencv.core.Point;

import java.util.Iterator;
import java.util.LinkedList;

public class PointerMotionDetector {
    LinkedList<Point> pointTrace;
    private static final int TRACE_FRAMES = 10;
    private static final double MOVING_AVERAGE_RATE = 0.8;

    // TODO: the thresholds should be defined dynamically
    private static final double STATIC_THRESHOLD = 30;
    private static final double LIFTING_DIST_THRESHOLD = 30;

    private Point movingAvePoint;

    public PointerMotionDetector() {
        pointTrace = new LinkedList<>();
        movingAvePoint = null;
    }

    public void addPoint(Point point) {
        pointTrace.addFirst(point);
        if (pointTrace.size() > TRACE_FRAMES) {
            pointTrace.removeLast();
        }
        if (movingAvePoint == null) {
            movingAvePoint = point;
        } else {
            movingAvePoint = new Point(movingAvePoint.x * MOVING_AVERAGE_RATE + point.x * (1-MOVING_AVERAGE_RATE),
                    movingAvePoint.y * MOVING_AVERAGE_RATE + point.y * (1-MOVING_AVERAGE_RATE));
        }
    }

    public boolean isStatic() {
        // The foot is considered static, if the foot finger point keeps in a relatively small range within TRACE_FRAMES frames:
        // The mean distance to moving average point < STATIC_THRESHOLD.
        double sqrDistMean = 0;
        for (Iterator<Point> iter = pointTrace.iterator(); iter.hasNext(); ) {
            Point p = iter.next();
            Point diff = new Point(p.x - movingAvePoint.x, p.y - movingAvePoint.y);
            sqrDistMean += diff.dot(diff) / pointTrace.size();
        }

        return (sqrDistMean < STATIC_THRESHOLD * STATIC_THRESHOLD);
    }

    public boolean isLifting() {
        // The foot is considered to be lifting, if the foot finger point moves up for LIFTING_DIST_THRESHOLD within TRACE_FRAMES frames.
        return (pointTrace.getLast().y - pointTrace.getFirst().y > LIFTING_DIST_THRESHOLD);
    }

}
