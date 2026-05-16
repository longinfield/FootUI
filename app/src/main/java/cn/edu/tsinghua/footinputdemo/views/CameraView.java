package cn.edu.tsinghua.footinputdemo.views;

import android.content.Context;

import android.os.Environment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;

import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.graphics.PixelFormat;
import android.view.ViewGroup;
import android.os.Build;
import android.view.Display;
import android.util.DisplayMetrics;
import android.graphics.Point;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.util.Date;

import cn.edu.tsinghua.footinputdemo.FootInputRecognizer;
import cn.edu.tsinghua.footinputdemo.MainActivity;
import cn.edu.tsinghua.footinputdemo.R;
import cn.edu.tsinghua.footinputdemo.SideBarService;

public class CameraView implements CameraBridgeViewBase.CvCameraViewListener {

    private Context mContext;
    private LinearLayout mCameraViewLayout;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mParams;
    public SideBarService mSideBarService;
    public Cursor mCursor;
    private LinearLayout mCursorLayout;
    private WindowManager.LayoutParams updateParams;

    FootInputRecognizer recognizer;
    JavaCameraView cameraView;
    VideoWriter videoWriter;
    String videoDir;
    Button button;
    boolean calibrated;

    public LinearLayout getView(Context context,
                                SideBarService sideBarService,
                                WindowManager windowManager) {
        mContext = context;
        mWindowManager = windowManager;
        mParams = new WindowManager.LayoutParams();
        mSideBarService=sideBarService;

        // compatible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        // set bg transparent
        mParams.format = PixelFormat.RGBA_8888;
        // can not focusable
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mParams.x = 600;
        mParams.y = 800;
        // window size
        mParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;

        // get layout
        LayoutInflater inflater = LayoutInflater.from(context);
        mCameraViewLayout= (LinearLayout) inflater.inflate(R.layout.layout_camera_view, null);

        calibrated = false;
        if (OpenCVLoader.initDebug()) {
            Log.i("footdemo", "working!");
        } else {
            Log.i("footdemo", "not working.");
        }

        /*Display defaultDisplay = mWindowManager.getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getSize(point);
        int screenWidth = point.x;
        int screenHeight = point.y;*/

        int mScreenDensity;
        int mScreenWidth;
        int mScreenHeight;

        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;

        recognizer = new FootInputRecognizer(mSideBarService,mScreenWidth,mScreenHeight,mScreenDensity,mWindowManager,mContext);
        cameraView = mCameraViewLayout.findViewById(R.id.cameraView);
        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        cameraView.setCvCameraViewListener(this);

        /*if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("footdemo", "camera not permitted");
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_CAMERA);
        } else {
            cameraView.setCameraPermissionGranted();
            cameraView.enableView();
        }*/
        if(MainActivity.camera_permitted){
            cameraView.setCameraPermissionGranted();
            cameraView.enableView();
        }

        button = mCameraViewLayout.findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (recognizer != null && !calibrated) {
                    recognizer.onClickInput();
                    button.setText(R.string.calibrated_text);
                    calibrated = true;
                    return;
                }

                if (calibrated) {
                    cameraView.disableView();
                    if (videoWriter != null) {
                        videoWriter.release();
                    }
                }
            }
        });

        /*if(MainActivity.camera_permitted==true){
            cameraView.setCameraPermissionGranted();
            cameraView.enableView();
        }*/

        mWindowManager.addView(mCameraViewLayout,mParams);
        return mCameraViewLayout;
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        recognizer.setSize(height, width);

        //mCursor=new Cursor();
        //mCursorLayout=mCursor.getView(mContext,mSideBarService,mWindowManager,mParams);
        //updateParams=mParams;

        boolean sdCardExist = Environment.getExternalStorageDirectory().canWrite();
        if (sdCardExist) {
            videoDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/OpenCV/";
            Log.d("footdemo_ext", videoDir);
            File videoDirFile = new File(videoDir);
            if(!videoDirFile.exists()){
                videoDirFile.mkdirs();
            }
        } else {
            Log.d("footdemo", "sd card inaccessible");
        }

        if (videoDir != null) {
            String filename = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".avi";
            Log.d("footdemo_ext", videoDir + filename);
            videoWriter = new VideoWriter(videoDir + filename, VideoWriter.fourcc('M', 'J', 'P', 'G'), 15, new Size(height, width));
        }
    }

    @Override
    public void onCameraViewStopped() {
        if (videoWriter != null) {
            videoWriter.release();
        }
    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {


        //Log.d("pointx",String.valueOf(SideBarService.pointx));
        //Log.d("pointy",String.valueOf(SideBarService.pointy));

        //updateParams.x=(int)SideBarService.pointx;
        //updateParams.y=(int)SideBarService.pointy;

        //mWindowManager.updateViewLayout(mCursorLayout,updateParams);
        //mSideBarService.mCursor.updateView(SideBarService.pointx,SideBarService.pointy);
        Core.rotate(inputFrame, inputFrame, Core.ROTATE_90_CLOCKWISE);
        Mat ret = recognizer.onCameraFrame(inputFrame);
        if (videoWriter != null) {
            videoWriter.write(ret);
        } else {
            Log.d("footdemo_ext", "writer not exist");
        }
        return ret;
    }
}
