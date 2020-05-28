package cn.edu.tsinghua.footinputdemo;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint3;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class SkinColorRecognizer {
    public static Scalar hsvRangeOffset = new Scalar(5, 50, 50);
    private Scalar rangeLow, rangeHigh;
    private boolean calibrated;
    private int channels;

    public SkinColorRecognizer() {
        calibrated = false;
    }

    public void setCalibrationImage(Mat image) {
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2HSV, 0);
        List<Mat> channel_imgs = new ArrayList<>();
        Core.split(image, channel_imgs);

        channels = channel_imgs.size();
        List<Integer> rangeLowList = new ArrayList<>();
        List<Integer> rangeHighList = new ArrayList<>();

        for (int i = 0; i < channels; i++) {
            Mat img = channel_imgs.get(i);
            Core.MinMaxLocResult result = Core.minMaxLoc(img);
            rangeLowList.add(Integer.valueOf((int) (result.minVal - hsvRangeOffset.val[i])));
            rangeHighList.add(Integer.valueOf((int) (result.maxVal + hsvRangeOffset.val[i])));
        }

        assert channels == 3;
        rangeLow = new Scalar(rangeLowList.get(0), rangeLowList.get(1), rangeLowList.get(2));
        rangeHigh = new Scalar(rangeHighList.get(0), rangeHighList.get(1), rangeHighList.get(2));

        Log.i("footdemo", "rangeLow: " + rangeLow.toString());
        Log.i("footdemo", "rangeHigh: " + rangeHigh.toString());
        calibrated = true;
    }

    public boolean needCalibration() {
        return !calibrated;
    }

    public Mat maskFrame(Mat frame) {
        frame = frame.clone();
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2HSV, 0);
        Mat mask = new Mat();
        Core.inRange(frame, rangeLow, rangeHigh, mask);
        return mask;
    }
}
