package cn.edu.tsinghua.footinputdemo.views;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import cn.edu.tsinghua.footinputdemo.R;
import cn.edu.tsinghua.footinputdemo.SideBarService;
import cn.edu.tsinghua.footinputdemo.utils.PermissionUtil;
import cn.edu.tsinghua.footinputdemo.utils.ScreenShotTool;




/**
 * Sidebar left & right
 *
 * @author majh
 */
public class SideBarContent implements View.OnClickListener {

    private Context mContext;
    //private boolean mLeft;
    private LinearLayout mContentView;
    private WindowManager mWindowManager;
    private LinearLayout mArrowView;
    private SideBarService mSideBarService;
    public ControlBar mControlBar;
    public LinearLayout mSeekBarView;
    private WindowManager.LayoutParams mParams;
    //private LinearLayout mAnotherArrowView;
    private int mTagTemp = -1;
    private ImageView imageView;

    private static final int COUNT_DOWN_TAG = 1;
    private static final int COUNT_DWON_TIME = 5000;
    private static final int PERMISSION_EXT_STORAGE = 2;

    private ScreenShotTool screenShotTool;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case COUNT_DOWN_TAG:
                    goNormal();
                    break;
            }
        }
    };

    LinearLayout getView(Context context,
                         WindowManager windowManager,
                         WindowManager.LayoutParams params,
                         LinearLayout arrowView,
                         SideBarService sideBarService) {
        mContext = context;
        mWindowManager = windowManager;
        mArrowView = arrowView;
        mSideBarService = sideBarService;
        mParams=params;

        int mScreenWidth;
        int mScreenHeight;
        int mScreenDensity;

        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;

        /*Display defaultDisplay = mWindowManager.getDefaultDisplay();
        Point outSize = new Point();
        defaultDisplay.getRealSize(outSize);
        */

        // get layout
        LayoutInflater inflater = LayoutInflater.from(context);
        mContentView = (LinearLayout) inflater.inflate(R.layout.layout_content, null);
        //int viewWidth=mContentView.getWidth();
        //Log.d("view Width",String.valueOf(viewWidth));
        //int semiwidth=(outSize.x-viewWidth)/2;
        //Log.d("screen width 1/2",String.valueOf(semiwidth));

        // init click
        mContentView.findViewById(R.id.layout_brightness).setOnClickListener(this);
        mContentView.findViewById(R.id.layout_back).setOnClickListener(this);
        mContentView.findViewById(R.id.layout_home).setOnClickListener(this);
        mContentView.findViewById(R.id.layout_screenshot).setOnClickListener(this);
        mContentView.findViewById(R.id.layout_easyaccess).setOnClickListener(this);
        //mContentView.findViewById(R.id.tv_annotation).setOnClickListener(this);
        mContentView.findViewById(R.id.layout_volume).setOnClickListener(this);
        mContentView.findViewById(R.id.layout_backstage).setOnClickListener(this);
        LinearLayout root = mContentView.findViewById(R.id.root);
        root.setPadding(15,0,0,0);

        mParams.x=0;
        mParams.y=0;
        mWindowManager.addView(mContentView,mParams);
        screenShotTool=new ScreenShotTool(mScreenWidth,mScreenHeight,mScreenDensity,mContext,mSideBarService);


        return mContentView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_brightness:
                removeOrSendMsg(true,true);
                brightnessPermissionCheck();
                break;
            case R.id.layout_back:
                removeOrSendMsg(true,true);
                clearSeekBar();
                mSideBarService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                break;
            case R.id.layout_home:
                removeOrSendMsg(true,false);
                goNormal();
                mSideBarService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
                break;
            case R.id.layout_screenshot:
                removeOrSendMsg(true,false);
                goNormal();
                //mSideBarService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT);
                //makeScreenShot();
                screenShotTool.createImageReader();
                Log.d("create","c");
                screenShotTool.startScreenShot();
                break;
            case R.id.layout_easyaccess:
                removeOrSendMsg(true,false);
                //goNormal();
                mSideBarService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
                break;
            /*case R.id.tv_annotation:
                removeOrSendMsg(true,false);
                goNormal();
                annotationGo();
                break;*/
            case R.id.layout_volume:
                removeOrSendMsg(true,true);
                brightnessOrVolume(1);
                break;
            case R.id.layout_backstage:
                removeOrSendMsg(true,false);
                goNormal();
                mSideBarService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
                break;
        }
    }

    private void brightnessOrVolume(int tag) {
        if(mTagTemp == tag) {
            if(null != mSeekBarView) {
                removeSeekBarView();
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
        mSeekBarView = mControlBar.getView(mContext,tag,this);
        mWindowManager.addView(mSeekBarView, mControlBar.mParams);
    }

    public void removeSeekBarView() {
        if(null != mSeekBarView) {
            mWindowManager.removeView(mSeekBarView);
            mSeekBarView = null;
        }
    }

    private void arrowsShow() {
        if(mContentView!=null){
            mContentView.setVisibility(View.GONE);
        }
        if(mArrowView!=null){
            mArrowView.setVisibility(View.VISIBLE);
        }

        //mAnotherArrowView.setVisibility(View.VISIBLE);
    }

    void clearSeekBar() {
        if(null != mSeekBarView) {
            mWindowManager.removeView(mSeekBarView);
            mSeekBarView = null;
        }
    }

    private void goNormal() {
        arrowsShow();
        clearSeekBar();
    }

    void removeOrSendMsg(boolean remove, boolean send) {
        if(remove) {
            mHandler.removeMessages(COUNT_DOWN_TAG);
        }
        if(send) {
            mHandler.sendEmptyMessageDelayed(COUNT_DOWN_TAG,COUNT_DWON_TIME);
        }
    }

    /**
     * when AccessibilityService is forced closed
     */
    void clearCallbacks() {
        if(null != mHandler) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private void brightnessPermissionCheck() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!PermissionUtil.isSettingsCanWrite(mContext)) {
                goNormal();
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
