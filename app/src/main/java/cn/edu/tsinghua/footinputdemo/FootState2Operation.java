package cn.edu.tsinghua.footinputdemo;

import android.util.Log;

import org.opencv.core.Point;

public class FootState2Operation {
    public interface FootOperationListener {
        void onLongTouch(Point pointer);
        void onDrag(Point dragPointer);
        void onPointing(Point pointer);
        void onTrigger(Point pointer);
        void onShortCutMenu();
        void onBack();
        void onHome();
        void onScreenshot();
        void onAppSwitch();
        void onVolume();
        void onNotice();
        void onEasyAccess();
        void onFlipScreenLeft();
        void onFlipScreenRight();
    };

    private FootOperationListener listener;
    public FootState2Operation(FootOperationListener listener) {
        this.listener = listener;
    }

    public void checkFootState(FootState state) {
        PointMotionDetector leftFinger = state.left.finger.motionDetector;
        PointMotionDetector rightFinger = state.right.finger.motionDetector;
        Point rightPoint = state.right.finger.smoothedPos;

        if (state.isFeetNear()) {
            boolean upperTouched = state.isPointNear(state.left.finger.position, state.right.finger.position);
            boolean lowerTouched = state.isPointNear(state.left.bottom.position, state.right.bottom.position);
            if (upperTouched && lowerTouched) {
                // App Switch = Double foot touch
                listener.onAppSwitch();
                state.clearAllDetection(1000);
            }
            if (!upperTouched && lowerTouched) {
                // Screenshot = Double foot lower touch
                listener.onScreenshot();
                state.clearAllDetection(1000);
            }
            if (upperTouched && !lowerTouched) {
                // Home = Double foot upper touch
                listener.onHome();
                state.clearAllDetection(1000);
            }
        } else {
            if (state.left.isLeaningLeft()) {
                if (state.right.isLeaningLeft()) {
                    // Flip Screen Left = Both foot left
                    listener.onFlipScreenLeft();
                    state.clearAllDetection(500);
                } else if (state.right.isLeaningRight()) {
                    // Easy Access = Left foot left + Right foot right
                    listener.onEasyAccess();
                    state.clearAllDetection(500);
                } else if (rightFinger.isStatic) {
                    // Back = Left foot left + Right foot static
                    listener.onBack();
                    state.clearAllDetection(500);
                }
            } else if (state.left.isLeaningRight()) {
                if (state.right.isLeaningRight()) {
                    // Flip Screen Right = Both foot right
                    listener.onFlipScreenRight();
                    state.clearClickDetection();
                }
            } else if (leftFinger.isClicking) {
                if (rightFinger.isClicking) {
                    // Notice = Left foot click + Right foot click
                    listener.onNotice();
                    state.clearAllDetection(300);
                } else if (!rightFinger.isReadyToClick) {
                    // Trigger = Left foot click + Right foot static
                    listener.onTrigger(rightPoint);
//                    state.clearClickDetection();
                    state.clearAllDetection(300);
                }
            } else if (leftFinger.isLongPressing) {
                if (rightFinger.isStatic) {
                    // Long Touch = Left foot press + Right foot static
                    listener.onLongTouch(rightPoint);
                } else {
                    // Drag = Left foot press + Right foot pointing
                    listener.onDrag(rightPoint);
                }
            } else if (leftFinger.isMovingUp) {
                if (rightFinger.isMovingUp) {
                    // Volume = Left foot lift + Right foot lift
                    listener.onVolume();
                    state.clearClickDetection();
                } else if (rightFinger.isStatic && !rightFinger.isInstantlyMovingUp) {
                    // Shortcut Menu = Left foot lift + Right foot static
                    listener.onShortCutMenu();
                    state.clearClickDetection(1000);
                }
            } else {
                listener.onPointing(rightPoint);
            }
        }
    }
}
