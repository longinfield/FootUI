package cn.edu.tsinghua.footinputdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener, Button.OnClickListener {
    private static final int PERMISSION_CAMERA = 1;
    private static final int PERMISSION_EXT_STORAGE = 2;
    FootInputRecognizer recognizer;
    JavaCameraView cameraView;
    VideoWriter videoWriter;
    String videoDir;
    Button button;
    boolean calibrated;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("opencv_java4");
    }

    @Override
    public void onClick(View v) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        calibrated = false;
        if (OpenCVLoader.initDebug()) {
            Log.i("footdemo", "working!");
        } else {
            Log.i("footdemo", "not working.");
        }

        recognizer = new FootInputRecognizer();
        cameraView = findViewById(R.id.cameraView);
        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        cameraView.setCvCameraViewListener(this);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("footdemo", "camera not permitted");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_CAMERA);
        } else {
            cameraView.setCameraPermissionGranted();
            cameraView.enableView();
        }

        button = findViewById(R.id.button);
        button.setOnClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("footdemo", "camera permitted");
                    cameraView.setCameraPermissionGranted();
                    cameraView.enableView();
                }
                return;
            }
        }
    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        Core.rotate(inputFrame, inputFrame, Core.ROTATE_90_CLOCKWISE);
        Mat ret = recognizer.onCameraFrame(inputFrame);
        if (videoWriter != null) {
            videoWriter.write(ret);
        } else {
            Log.d("footdemo_ext", "writer not exist");
        }
        return ret;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        recognizer.setSize(height, width);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("footdemo", "write ext storage not permitted");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_EXT_STORAGE);
        }

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
}
