package cn.edu.tsinghua.footinputdemo;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;

import cn.edu.tsinghua.footinputdemo.views.CameraView;
import cn.edu.tsinghua.footinputdemo.views.Cursor;
import cn.edu.tsinghua.footinputdemo.views.ScrollButton;
import cn.edu.tsinghua.footinputdemo.views.SideBarArrow;
import cn.edu.tsinghua.footinputdemo.views.ScrollButton;
/**
 * a service to help user simulate click event
 *
 * @author majh
 */
public class SideBarService extends AccessibilityService {

    private SideBarHideReceiver mReceiver;
    public SideBarArrow mArrowBar;
    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;
    public Cursor mCursor;
    private LinearLayout mCursorLayout;
    private LinearLayout mScrollButtonLayout;
    private CameraView mCameraView;
    private ScrollButton mScrollButton;
    public static Intent mResultData = null;
    public static double pointx,pointy;


    private static final String ACTION_HIDE = "com.xunfeivr.maxsidebar.ACTION_HIDE";

    @Override
    public void onCreate() {
        Log.d("added","add");
        super.onCreate();
        createToucher();

        //createCameraCursor();
        createCameraView();
        createCursor();
        createScrollButton();

    }

    @SuppressLint({"RtlHardcoded", "InflateParams", "ClickableViewAccessibility"})
    private void createToucher() {
        // get window manager
        mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        // create arrow
        mArrowBar = new SideBarArrow();
        LinearLayout mArrow = mArrowBar.getView(this, mWindowManager, this);

        // register
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_HIDE);
        mReceiver = new SideBarHideReceiver();
        mReceiver.setSideBar(mArrowBar);
        registerReceiver(mReceiver, filter);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void createCursor(){
        //create cursor
        mParams = new WindowManager.LayoutParams();
        // compatible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        }
        // set bg transparent
        mParams.format = PixelFormat.RGBA_8888;
        // can not focusable
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mParams.x = 0;
        mParams.y = 1000;

        mParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;

        mCursor=new Cursor();
        mCursorLayout=mCursor.getView(this,this,mWindowManager,mParams);

    }

    private void createCameraView(){
        mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        mCameraView = new CameraView();
        LinearLayout mCamera_ViewLayout =  mCameraView.getView(this,this, mWindowManager);
    }

    private void createScrollButton() {
        // get window manager
        mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        // create arrow
        mScrollButton = new ScrollButton();
        LinearLayout mScrollButtonLayout = mScrollButton.getView(this, mWindowManager, this);

    }



    @Override
    public void onDestroy() {
        mArrowBar.clearAll();
        mWindowManager.removeView(mCursorLayout);
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }

    public static void setResultData(Intent mResultData) {
        SideBarService.mResultData = mResultData;
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
    }
}
