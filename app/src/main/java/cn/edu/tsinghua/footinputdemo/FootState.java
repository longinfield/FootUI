package cn.edu.tsinghua.footinputdemo;

import org.opencv.core.Point;

public class FootState {
    // Single foot descriptor
    public static final int IS_LEANING_LEFT = 0x01;
    public static final int IS_LEANING_RIGHT = 0x02;
    public static final int IS_LIFTING = 0x04;
    public static final int IS_PRESSING = 0x08;
    public static final int IS_CLICKING = 0x10;
    public static final int IS_STATIC = 0x20;

    // Both feet descriptor
    public static final int IS_TOUCHING = 0x01;
    public static final int IS_APPROACHING = 0x02;
    public static final int IS_SEPARATING = 0x04;

    public int left;
    public int right;
    public int both;
    public Point leftPoint;
    public Point rightPoint;

    public FootState() {
        left = 0;
        right = 0;
        both = 0;
    }

    public boolean isLeaningLeft(int foot) {
        return (foot & IS_LEANING_LEFT) != 0;
    }

    public boolean isLeaningRight(int foot) {
        return (foot & IS_LEANING_RIGHT) != 0;
    }

    public boolean isLifting(int foot) {
        return (foot & IS_LIFTING) != 0;
    }

    public boolean isPressing(int foot) {
        return (foot & IS_PRESSING) != 0;
    }

    public boolean isClicking(int foot) {
        return (foot & IS_CLICKING) != 0;
    }

    public boolean isStatic(int foot) {
        return (foot & IS_STATIC) != 0;
    }

    public boolean isTouching() {
        return (both & IS_TOUCHING) != 0;
    }

    public boolean isApproaching() {
        return (both & IS_APPROACHING) != 0;
    }

    public boolean isSeparating() {
        return (both & IS_SEPARATING) != 0;
    }

    private String stateToString(int state) {
        String ret = "";
        if ((state & IS_STATIC) > 0) {
            ret += " static";
        }
        if ((state & IS_CLICKING) > 0) {
            ret += " clicking";
        }
        if ((state & IS_PRESSING) > 0) {
            ret += " pressing";
        }
        if ((state & IS_LIFTING) > 0) {
            ret += " lifting";
        }
        if ((state & IS_LEANING_RIGHT) > 0) {
            ret += " right";
        }
        if ((state & IS_LEANING_LEFT) > 0) {
            ret += " left";
        }
        if ((state & IS_TOUCHING) > 0) {
            ret += " touching";
        }
        if ((state & IS_APPROACHING) > 0) {
            ret += " approaching";
        }
        if ((state & IS_SEPARATING) > 0) {
            ret += " separating";
        }

        return ret;
    }

    public String toString() {
        return  "L:" + stateToString(left) + ", R:" + stateToString(right);
    }
}
