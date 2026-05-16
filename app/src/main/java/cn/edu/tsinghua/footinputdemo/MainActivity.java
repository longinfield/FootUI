package cn.edu.tsinghua.footinputdemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import android.widget.Toast;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.content.Intent;
import android.provider.Settings;
import android.os.Build;

import cn.edu.tsinghua.footinputdemo.utils.PermissionUtil;

public class MainActivity extends AppCompatActivity{
    private static final int PERMISSION_CAMERA = 1;
    private static final int PERMISSION_EXT_STORAGE = 2;
    private AppCompatButton mFlastWindowButton;
    private AppCompatButton mAccessibilityButton;
    private static final int FLAT_REQUEST_CODE = 213;
    private static final int ACCESSIBILITY_REQUEST_CODE = 438;
    public static final int REQUEST_MEDIA_PROJECTION = 18;
    public static boolean camera_permitted;
    public static boolean external_storage;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("opencv_java4");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mFlastWindowButton = findViewById(R.id.btn_flatwindow);
        mAccessibilityButton = findViewById(R.id.btn_accessibility);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("footdemo", "camera not permitted");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_CAMERA);
            //camera_permitted=true;
        } else {
            camera_permitted=true;
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("footdemo", "write ext storage not permitted");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_EXT_STORAGE);
            //external_storage=true;
        }else{
            external_storage=true;
        }

        flatWindowVisible();
        requestCapturePermission();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("footdemo", "camera permitted");
                    camera_permitted=true;
                    //cameraView.setCameraPermissionGranted();
                    //cameraView.enableView();
                }
                break;
            }
            case PERMISSION_EXT_STORAGE:{
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("footdemo", "external_storage permitted");
                    external_storage=true;
                    //cameraView.setCameraPermissionGranted();
                    //cameraView.enableView();
                }
            }
        }
    }

    public void requestCapturePermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //5.0 之后才允许使用屏幕截图

            return;
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("footdemo", "write ext storage not permitted");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_EXT_STORAGE);
        }

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION);
    }

    private void flatWindowVisible() {
        Log.d("flatwindow","visible");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // > M,grant permission
            if (PermissionUtil.isCanDrawOverlays(this)) {
                // permission authorized,service go,button gone
                mFlastWindowButton.setVisibility(View.GONE);
                accessibilityVisible();
            } else {
                // permission unauthorized,button visible
                mFlastWindowButton.setVisibility(View.VISIBLE);
                Toast.makeText(this,getString(R.string.permission_flatwindow_),Toast.LENGTH_SHORT).show();
            }
        } else {
            // < M,service go,gone
            mFlastWindowButton.setVisibility(View.GONE);
            accessibilityVisible();
        }
    }

    private void accessibilityVisible() {
        if(PermissionUtil.isAccessibilityServiceEnable(this)) {
            android.widget.Toast.makeText(this,getString(R.string.permission_notice),Toast.LENGTH_SHORT).show();
            //Intent intent = new Intent(this, CursorService.class);
            //startService(intent);
            finish();
        }else {
            mAccessibilityButton.setVisibility(View.VISIBLE);
            Toast.makeText(this,getString(R.string.permission_accessibility_),Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)//表示注解目标只能够在指定的版本及以上API运行，消除高版本Api在低版本SDK上的报错，作用上和TargetApi相同，在低于目标版本API下运行一样会报错。从官方的表述可以看出更推荐使用RequiresApi替换TargetApi。建议在方法内部做版本判断，来确保运行不会出问题。

    public void goGetFlatWindow(View view) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        startActivityForResult(intent,FLAT_REQUEST_CODE);
    }

    public void goGetAccessibility(View view) {
        Intent accessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(accessibleIntent,ACCESSIBILITY_REQUEST_CODE);
    }
    //在一个Activity中，可能会使用startActivityForResult()方法打开多个不同的Activity处理不同的业务，当这些新Activity关闭后，系统都会调用前面Activity的onActivityResult(int requestCode, int resultCode, Intent data)方法。为了知道返回的数据来自于哪个新Activity，在onActivityResult()方法中可以这样做(ResultActivity和NewActivity为要打开的新Activity)。

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == FLAT_REQUEST_CODE) {
            flatWindowVisible();
        }else if(requestCode == ACCESSIBILITY_REQUEST_CODE){
            accessibilityVisible();
        }else if(requestCode==REQUEST_MEDIA_PROJECTION){
            if (resultCode == RESULT_OK && data != null){
                SideBarService.setResultData(data);
                Log.d("Result OK",data.toString());
            }
        }
    }

}
