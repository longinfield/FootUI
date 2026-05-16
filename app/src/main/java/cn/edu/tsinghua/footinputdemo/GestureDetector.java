package cn.edu.tsinghua.footinputdemo;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

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

import cn.edu.tsinghua.footinputdemo.utils.PermissionUtil;
import cn.edu.tsinghua.footinputdemo.utils.ScreenShotTool;
import cn.edu.tsinghua.footinputdemo.views.ControlBar;
import cn.edu.tsinghua.footinputdemo.views.SideBarContent;


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

    public SideBarService msidebarservice;

    private FootState2Operation operation;
    private FootEllipse prevEllipseL, prevEllipseR;
    private Mat tempFrame;
    private FootState footState;
    private Point initRightPoint;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;
    private WindowManager mWindowManager;
    private Context mContext;
    private int mTagTemp = -1;
    private ControlBar mControlBar;
    private LinearLayout mSeekBarView;
    private SideBarContent mSideBarContent;
    private ScreenShotTool screenShotTool;
    private boolean screenshotFlag;
    private boolean longTouchFlag;
    private boolean dragFlag;

    private int dragStartx;
    private int dragStarty;
    private int dragEndx;
    private int dragEndy;


    public GestureDetector(SideBarService sidebarservice, int screenWidth, int screenHeight, int screenDensity,WindowManager windowManager, Context context) {
        msidebarservice=sidebarservice;
        mScreenWidth=screenWidth;
        mScreenHeight=screenHeight;
        mScreenDensity=screenDensity;
        prevEllipseL = prevEllipseR = null;
        mWindowManager=windowManager;
        mContext=context;
        mSideBarContent=new SideBarContent();
        screenshotFlag=false;
        longTouchFlag=false;
        dragFlag=false;
        screenShotTool=new ScreenShotTool(mScreenWidth,mScreenHeight,mScreenDensity,mContext, msidebarservice);
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


            footState.left.angle = Math.toDegrees(Math.atan2(-ellipseL.mainDir.y, ellipseL.mainDir.x));
            footState.right.angle = Math.toDegrees(Math.atan2(-ellipseR.mainDir.y, ellipseR.mainDir.x));
            footState.left.boundingBox = ellipseL.boundingBox;
            footState.right.boundingBox = ellipseR.boundingBox;

            Point[] leftPoints = {ellipseL.center, ellipseL.finger, ellipseL.bottom};
            Point[] rightPoints = {ellipseR.center, ellipseR.finger, ellipseR.bottom};
            initRightPoint=ellipseR.finger;
            footState.update(System.currentTimeMillis(), leftPoints, rightPoints, touched);
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

        if(dragFlag){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    GestureDescription.Builder builder = new GestureDescription.Builder();
                    Path path = new Path();
                    path.moveTo((int)dragStartx, (int)dragStarty);
                    path.lineTo((int)dragEndx, (int)dragEndy);

                    builder.addStroke(new GestureDescription.StrokeDescription(path, 10, 500));
                    final GestureDescription gestureDescription = builder.build();
                    msidebarservice.dispatchGesture(gestureDescription, new AccessibilityService.GestureResultCallback() {
                        @Override
                        public void onCompleted(GestureDescription gestureDescription) {
                            super.onCompleted(gestureDescription);
                            int count=gestureDescription.getStrokeCount();
                            Log.d("dispatch", "----模拟手势成功-----");
                            Log.d("dispatch",String.valueOf(count));
                        }

                        @Override
                        public void onCancelled(GestureDescription gestureDescription) {
                            super.onCancelled(gestureDescription);
                            Log.d("dispatch", "----模拟手势失败-----");
                        }
                    }, null);
                }
            }).start();
            Log.d("simulate drag","模拟拖拽来自长按");
            dragFlag=false;
        }else{
            int[] location = new int[2];
            msidebarservice.mCursor.cursor.getLocationOnScreen(location);
            final int posx = location[0];
            final int posy = location[1];

            if(msidebarservice!=null&&!longTouchFlag){
                Handler handler=new Handler(msidebarservice.getMainLooper());

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        GestureDescription.Builder builder = new GestureDescription.Builder();
                        Path path = new Path();
                        path.moveTo(posx-10, posy+10);
                        path.lineTo(posx-10, posy+10);

                        builder.addStroke(new GestureDescription.StrokeDescription(path, 10, 4000));
                        final GestureDescription gestureDescription = builder.build();
                        msidebarservice.dispatchGesture(gestureDescription, new AccessibilityService.GestureResultCallback() {
                            @Override
                            public void onCompleted(GestureDescription gestureDescription) {
                                super.onCompleted(gestureDescription);
                                int count=gestureDescription.getStrokeCount();
                                Log.d("dispatch", "----模拟手势成功-----");
                                Log.d("dispatch",String.valueOf(count));
                            }

                            @Override
                            public void onCancelled(GestureDescription gestureDescription) {
                                super.onCancelled(gestureDescription);
                                Log.d("dispatch", "----模拟手势失败-----");
                            }
                        }, null);
                        Log.d("mainloop","uithread running");
                    }
                });
                longTouchFlag=true;
            }
        }

        writeTextToTempFrame("Long Touch: " + point.toString());
        Imgproc.circle(tempFrame, point, 20, new Scalar(255, 0, 0), 5);
    }

    @Override
    public void onAppSwitch() {
        msidebarservice.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS);
        writeTextToTempFrame("App Switch");
    }

    @Override
    public void onBack() {
        if(null != mSeekBarView) {
            removeSeekBarView();
        }
        else if(msidebarservice.mArrowBar!=null && msidebarservice.mArrowBar.mContentBar!=null && msidebarservice.mArrowBar.mContentBar.mSeekBarView!=null){
            msidebarservice.mArrowBar.mContentBar.removeSeekBarView();
        }else{
            msidebarservice.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
            writeTextToTempFrame("Back");
        }
    }

    @Override
    public void onDrag(Point dragPointer) {
        int[] location = new int[2];

        final double x=0+(dragPointer.x-initRightPoint.x)*10;
        final double y=1000+(dragPointer.y-initRightPoint.y)*24;

        if(msidebarservice!=null){
            Handler handler=new Handler(msidebarservice.getMainLooper());

            handler.post(new Runnable() {

                @Override
                public void run() {
                    msidebarservice.mCursor.updateView(x,y);
                    Log.d("mainloop","uithread running");
                }
            });

            msidebarservice.mCursor.cursor.getLocationOnScreen(location);
            final int posx = location[0];
            final int posy= location[1];

            if(!dragFlag){
                dragStartx = posx;
                dragStarty = posy;
                Log.d("dragstartx",String.valueOf(dragStartx));
                Log.d("dragstarty",String.valueOf(dragStarty));
                dragFlag=true;
            }else{
                dragEndx = posx;
                dragEndy = posy;
                Log.d("dragendx",String.valueOf(dragStartx));
                Log.d("dragendy",String.valueOf(dragStarty));
            }
        }
        writeTextToTempFrame("Drag: " + dragPointer.toString());
        Imgproc.circle(tempFrame, dragPointer, 20, new Scalar(255, 0, 0), 5);
    }

    @Override
    public void onEasyAccess() {
        msidebarservice.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
        writeTextToTempFrame("Easy Access");
    }

    @Override
    public void onFlipScreenLeft() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                //path.moveTo(posx, posy);
                GestureDescription.Builder builder = new GestureDescription.Builder();
                Path path = new Path();
                path.moveTo(1000, 1000);
                path.lineTo(10, 1000);

                builder.addStroke(new GestureDescription.StrokeDescription(path, 10, 500));
                final GestureDescription gestureDescription = builder.build();
                msidebarservice.dispatchGesture(gestureDescription, new AccessibilityService.GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        super.onCompleted(gestureDescription);
                        int count=gestureDescription.getStrokeCount();
                        Log.d("dispatch", "----模拟手势成功-----");
                        Log.d("dispatch",String.valueOf(count));
                    }

                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        super.onCancelled(gestureDescription);
                        Log.d("dispatch", "----模拟手势失败-----");
                    }
                }, null);
            }
        }).start();

        writeTextToTempFrame("Flip Screen Left");
    }

    @Override
    public void onFlipScreenRight() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //path.moveTo(posx, posy);
                GestureDescription.Builder builder = new GestureDescription.Builder();
                Path path = new Path();
                path.moveTo(10, 1000);
                path.lineTo(1000, 1000);

                builder.addStroke(new GestureDescription.StrokeDescription(path, 10, 500));
                final GestureDescription gestureDescription = builder.build();
                msidebarservice.dispatchGesture(gestureDescription, new AccessibilityService.GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        super.onCompleted(gestureDescription);
                        int count=gestureDescription.getStrokeCount();
                        Log.d("dispatch", "----模拟手势成功-----");
                        Log.d("dispatch",String.valueOf(count));
                    }

                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        super.onCancelled(gestureDescription);
                        Log.d("dispatch", "----模拟手势失败-----");
                    }
                }, null);
            }
        }).start();
        writeTextToTempFrame("Flip Screen Right");
    }

    @Override
    public void onHome() {
        if(msidebarservice!=null){
            Handler handler=new Handler(msidebarservice.getMainLooper());

            handler.post(new Runnable() {

                @Override
                public void run() {
                    msidebarservice.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
                    Log.d("mainloop","uithread running");
                }
            });
        }

        writeTextToTempFrame("Home");
    }

    @Override
    public void onNotice() {
        if(msidebarservice!=null){
            Handler handler=new Handler(msidebarservice.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    brightnessPermissionCheck();
                    Log.d("mainloop","uithread running");
                }
            });
        }

        writeTextToTempFrame("Brightness");
    }

    @Override
    public void onPointing(Point pointer) {
        //final double x=Math.min(Math.max((pointer.x-initRightPoint.x)*5,0),1280);
        //final double y=Math.min(Math.max((pointer.x-initRightPoint.y)*5,0),1920);
        if(screenshotFlag){
            screenshotFlag=false;
        }
        if(longTouchFlag){
            longTouchFlag=false;
        }


        if(dragFlag){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    GestureDescription.Builder builder = new GestureDescription.Builder();
                    Path path = new Path();
                    path.moveTo((int)dragStartx, (int)dragStarty);
                    path.lineTo((int)dragEndx, (int)dragEndy);

                    builder.addStroke(new GestureDescription.StrokeDescription(path, 10, 500));
                    final GestureDescription gestureDescription = builder.build();
                    msidebarservice.dispatchGesture(gestureDescription, new AccessibilityService.GestureResultCallback() {
                        @Override
                        public void onCompleted(GestureDescription gestureDescription) {
                            super.onCompleted(gestureDescription);
                            int count=gestureDescription.getStrokeCount();
                            Log.d("dispatch", "----模拟手势成功-----");
                            Log.d("dispatch",String.valueOf(count));
                        }

                        @Override
                        public void onCancelled(GestureDescription gestureDescription) {
                            super.onCancelled(gestureDescription);
                            Log.d("dispatch", "----模拟手势失败-----");
                        }
                    }, null);
                }
            }).start();
            Log.d("simulate drag","模拟拖拽来自长按");
            dragFlag=false;
        }

        final double x=0+(pointer.x-initRightPoint.x)*10;
        final double y=1000+(pointer.y-initRightPoint.y)*24;

        if(msidebarservice!=null){
            Handler handler=new Handler(msidebarservice.getMainLooper());

            handler.post(new Runnable() {

                @Override
                public void run() {
                    msidebarservice.mCursor.updateView(x,y);
                    Log.d("mainloop","uithread running");

                    if(mSeekBarView!=null){
                        float scale,progress;
                        final int max;
                        max=mControlBar.seekBar.getMax();
                        int[] location = new int[2];
                        msidebarservice.mCursor.cursor.getLocationOnScreen(location);
                        final int posx = location[0];

                        scale=Math.min(1,(Math.max(0,((float)(posx-300)/(float)(mScreenWidth-600)))));
                        Log.d("posx",String.valueOf(posx));
                        Log.d("scale",String.valueOf(scale));
                        progress=scale*max;
                        mControlBar.seekBar.setProgress((int)progress);
                    }
                    if(msidebarservice.mArrowBar!=null && msidebarservice.mArrowBar.mContentBar!=null && msidebarservice.mArrowBar.mContentBar.mSeekBarView!=null){
                        float scale,progress;
                        final int max;
                        max=msidebarservice.mArrowBar.mContentBar.mControlBar.seekBar.getMax();
                        int[] location = new int[2];
                        msidebarservice.mCursor.cursor.getLocationOnScreen(location);
                        final int posx = location[0];

                        scale=Math.min(1,(Math.max(0,((float)(posx-300)/(float)(mScreenWidth-600)))));
                        Log.d("posx",String.valueOf(posx));
                        Log.d("scale",String.valueOf(scale));
                        progress=scale*max;
                        msidebarservice.mArrowBar.mContentBar.mControlBar.seekBar.setProgress((int)progress);
                    }


                }
            });
        }

        //Log.d("Init_right_point x",String.valueOf(initRightPoint.x));
        //Log.d("Init_right_point y",String.valueOf(initRightPoint.y));

        writeTextToTempFrame("Pointing: " + pointer.toString());
        Imgproc.circle(tempFrame, pointer, 20, new Scalar(255, 0, 0), 5);
    }

    @Override
    public void onScreenshot() {
        if(!screenshotFlag && screenShotTool!=null){
            Log.d("screenshotflag",String.valueOf(screenshotFlag));
            screenShotTool.createImageReader();
            Log.d("create","c");
            screenShotTool.startScreenShot();
            screenshotFlag=true;
        }
        writeTextToTempFrame("Screen Shot");
    }

    @Override
    public void onShortCutMenu() {
        Log.d("screenwidth",String.valueOf(mScreenWidth));
        Log.d("screenheight",String.valueOf(mScreenHeight));

        if(dragFlag){
            dragFlag=false;
            Log.d("simulate dragshort","模拟拖拽来自short");
        }
        else{
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //path.moveTo(posx, posy);
                    GestureDescription.Builder builder = new GestureDescription.Builder();
                    Path path = new Path();
                    path.moveTo(20, mScreenHeight-5);
                    path.lineTo(20, mScreenHeight-5);

                    builder.addStroke(new GestureDescription.StrokeDescription(path, 10, 500));
                    final GestureDescription gestureDescription = builder.build();
                    msidebarservice.dispatchGesture(gestureDescription, new AccessibilityService.GestureResultCallback() {
                        @Override
                        public void onCompleted(GestureDescription gestureDescription) {
                            super.onCompleted(gestureDescription);
                            int count=gestureDescription.getStrokeCount();
                            Log.d("dispatch", "----模拟手势成功-----");
                            Log.d("dispatch",String.valueOf(count));
                        }

                        @Override
                        public void onCancelled(GestureDescription gestureDescription) {
                            super.onCancelled(gestureDescription);
                            Log.d("dispatch", "----模拟手势失败-----");
                        }
                    }, null);
                }
            }).start();

            writeTextToTempFrame("Short Cut Menu");
        }

    }

    @Override
    public void onTrigger(Point pointer) {

        int[] location = new int[2];
        msidebarservice.mCursor.cursor.getLocationOnScreen(location);
        final int posx = location[0];
        final int posy = location[1];

        if(msidebarservice!=null){
            Handler handler=new Handler(msidebarservice.getMainLooper());

            handler.post(new Runnable() {

                @Override
                public void run() {
                    GestureDescription.Builder builder = new GestureDescription.Builder();
                    Path path = new Path();
                    path.moveTo(posx-10, posy+10);
                    path.lineTo(posx-10, posy+10);

                    builder.addStroke(new GestureDescription.StrokeDescription(path, 10, 200));
                    final GestureDescription gestureDescription = builder.build();
                    msidebarservice.dispatchGesture(gestureDescription, new AccessibilityService.GestureResultCallback() {
                        @Override
                        public void onCompleted(GestureDescription gestureDescription) {
                            super.onCompleted(gestureDescription);
                            int count=gestureDescription.getStrokeCount();
                            Log.d("dispatch", "----模拟手势成功-----");
                            Log.d("dispatch",String.valueOf(count));
                        }

                        @Override
                        public void onCancelled(GestureDescription gestureDescription) {
                            super.onCancelled(gestureDescription);
                            Log.d("dispatch", "----模拟手势失败-----");
                        }
                    }, null);
                    Log.d("mainloop","uithread running");
                }
            });
        }

        /*new Thread(new Runnable() {
            @Override
            public void run() {
                //path.moveTo(posx, posy);

            }
        }).start();*/

        writeTextToTempFrame("Trigger: " + pointer.toString());
        Imgproc.circle(tempFrame, pointer, 20, new Scalar(255, 0, 0), 5);
    }

    @Override
    public void onVolume() {
        if(msidebarservice!=null){
            Handler handler=new Handler(msidebarservice.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    brightnessOrVolume(1);
                    Log.d("mainloop","uithread running");
                }
            });
        }
        writeTextToTempFrame("Volume");
    }

    private void brightnessOrVolume(int tag) {
        if(mTagTemp == tag) {
            if(null != mSeekBarView) {
                //removeSeekBarView();
            }else {
                addSeekBarView(tag);
            }
            return;
        }
        mTagTemp = tag;
        if(null == mControlBar) {
            mControlBar = new ControlBar();
        }
        if(null == mSeekBarView) {
            addSeekBarView(tag);
        }else {
            removeSeekBarView();
            addSeekBarView(tag);
        }
    }

    private void addSeekBarView(int tag) {
        mSeekBarView = mControlBar.getView(mContext,tag,mSideBarContent);
        mWindowManager.addView(mSeekBarView, mControlBar.mParams);
    }

    private void removeSeekBarView() {
        if(null != mSeekBarView) {
            mWindowManager.removeView(mSeekBarView);
            mSeekBarView = null;
        }
    }

    /*private void arrowsShow() {
        mContentView.setVisibility(View.GONE);
        mArrowView.setVisibility(View.VISIBLE);
        //mAnotherArrowView.setVisibility(View.VISIBLE);
    }*/

    void clearSeekBar() {
        if(null != mSeekBarView) {
            mWindowManager.removeView(mSeekBarView);
            mSeekBarView = null;
        }
    }

    /*private void goNormal() {
        arrowsShow();
        clearSeekBar();
    }*/

    /*private void annotationGo() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.notes", "com.android.notes.MainActivity"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            mContext.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(mContext, mContext.getString(R.string.app_not_find), Toast.LENGTH_SHORT).show();
        }
    }*/

    /*void removeOrSendMsg(boolean remove, boolean send) {
        if(remove) {
            mHandler.removeMessages(COUNT_DOWN_TAG);
        }
        if(send) {
            mHandler.sendEmptyMessageDelayed(COUNT_DOWN_TAG,COUNT_DWON_TIME);
        }
    }*/

    /**
     * when AccessibilityService is forced closed
     */
    /*void clearCallbacks() {
        if(null != mHandler) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }*/

    /*private void makeScreenShot() {
        Intent launch = new Intent(mContext,ScreenShot.class);
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(launch);

        //imageView.setImageBitmap(ScreenShotUtil.addBlackBoard(bitmap));

    }*/

    @SuppressLint("ObsoleteSdkInt")
    private void brightnessPermissionCheck() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!PermissionUtil.isSettingsCanWrite(mContext)) {
                //goNormal();
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + mContext.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                Toast.makeText(mContext,mContext.getString(R.string.setting_modify_toast),Toast.LENGTH_LONG).show();
            }else {
                brightnessOrVolume(0);
            }
        }else {
            brightnessOrVolume(0);
        }
    }

}
