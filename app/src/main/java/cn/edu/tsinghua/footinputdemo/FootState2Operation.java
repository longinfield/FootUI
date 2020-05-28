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

    public void checkFootState(FootState state, Point leftPoint, Point rightPoint) {
        Log.i("footdemo", state.toString());
        if (state.isTouching()) {
            if (state.isLeaningLeft(state.left) && state.isLeaningRight(state.right)) {
                // Screenshot = Double foot lower touch
                listener.onScreenshot();
            } else if (state.isLeaningRight(state.left) && state.isLeaningLeft(state.right)) {
                // Home = Double foot upper touch
                listener.onHome();
            } else {
                // Double foot touch
                listener.onAppSwitch();
            }
        } else {
            if (state.isLeaningLeft(state.left)) {
                if (state.isLeaningLeft(state.right)) {
                    // Flip Screen Left = Both foot left
                    listener.onFlipScreenLeft();
                } else if (state.isLeaningRight(state.right)) {
                    // Easy Access = Left foot left + Right foot right + foot separating (might confuse with Screenshot)
                    if (state.isSeparating()) {
                        listener.onEasyAccess();
                    }
                } else if (state.isStatic(state.right)) {
                    // Back = Left foot left + Right foot static
                    listener.onBack();
                }
            } else if (state.isLeaningRight(state.left)) {
                if (state.isLeaningRight(state.right)) {
                    // Flip Screen Right = Both foot right
                    listener.onFlipScreenRight();
                }
            } else if (state.isLifting(state.left)) {
                if (state.isLifting(state.right)) {
                    // Volume = Left foot lift + Right foot lift
                    listener.onVolume();
                } else if (state.isStatic(state.right)) {
                    // Shortcut Menu = Left foot lift + Right foot static
                    listener.onShortCutMenu();
                }
            } else if (state.isPressing(state.left)) {
                if (state.isStatic(state.right)) {
                    // Long Touch = Left foot press + Right foot static
                    listener.onLongTouch(rightPoint);
                } else {
                    // Drag = Left foot press + Right foot pointing
                    listener.onDrag(rightPoint);
                }
            } else if (state.isClicking(state.left)) {
                if (state.isClicking(state.right)) {
                    // Notice = Left foot click + Right foot click
                    listener.onNotice();
                } else if (state.isStatic(state.right)) {
                    // Trigger = Left foot click + Right foot static
                    listener.onTrigger(rightPoint);
                }
            } else {
                listener.onPointing(rightPoint);
            }
        }
    }
}
