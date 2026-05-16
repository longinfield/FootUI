package cn.edu.tsinghua.footinputdemo.views;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import cn.edu.tsinghua.footinputdemo.R;
import cn.edu.tsinghua.footinputdemo.SideBarService;

public class ScrollButton {
    private WindowManager.LayoutParams mParams;
    private LinearLayout mScrollButtonView;
    private Context mContext;
    private WindowManager mWindowManager;
    private SideBarService mSideBarService;

    public LinearLayout getView(Context context, WindowManager windowManager, SideBarService sideBarService) {
        mContext = context;
        mWindowManager = windowManager;
        mSideBarService = sideBarService;
        mParams = new WindowManager.LayoutParams();
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
        mParams.x = 0;
        mParams.y = 0;
        // window size
        mParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        // get layout
        LayoutInflater inflater = LayoutInflater.from(context);
        mScrollButtonView = (LinearLayout) inflater.inflate(R.layout.layout_scroll_button, null);
        AppCompatImageView scrollUp = mScrollButtonView.findViewById(R.id.scrollUp);
        AppCompatImageView scrollDown = mScrollButtonView.findViewById(R.id.scrollDown);
        scrollUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //path.moveTo(posx, posy);
                    GestureDescription.Builder builder = new GestureDescription.Builder();
                    Path path = new Path();
                    path.moveTo(20, 2000);
                    path.lineTo(20, 200);
                    //path.moveTo(posx-10, posy+10);

                    builder.addStroke(new GestureDescription.StrokeDescription(path, 10, 500));
                    final GestureDescription gestureDescription = builder.build();
                    mSideBarService.dispatchGesture(gestureDescription, new AccessibilityService.GestureResultCallback() {
                        @Override
                        public void onCompleted(GestureDescription gestureDescription) {
                            super.onCompleted(gestureDescription);
                            int count=gestureDescription.getStrokeCount();
                            Log.d("Scroll", "----模拟手势成功-----");
                            Log.d("Scroll",String.valueOf(count));
                        }

                        @Override
                        public void onCancelled(GestureDescription gestureDescription) {
                            super.onCancelled(gestureDescription);
                            Log.d("Scroll", "----模拟手势失败-----");
                        }
                    }, null);
                }
            }).start();

            }
        });
        scrollDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //path.moveTo(posx, posy);
                        GestureDescription.Builder builder = new GestureDescription.Builder();
                        Path path = new Path();
                        path.moveTo(20, 300);
                        path.lineTo(20, 2000);
                        //path.moveTo(posx-10, posy+10);

                        builder.addStroke(new GestureDescription.StrokeDescription(path, 10, 500));
                        final GestureDescription gestureDescription = builder.build();
                        mSideBarService.dispatchGesture(gestureDescription, new AccessibilityService.GestureResultCallback() {
                            @Override
                            public void onCompleted(GestureDescription gestureDescription) {
                                super.onCompleted(gestureDescription);
                                int count=gestureDescription.getStrokeCount();
                                Log.d("Scroll", "----模拟手势成功-----");
                                Log.d("Scroll",String.valueOf(count));
                            }

                            @Override
                            public void onCancelled(GestureDescription gestureDescription) {
                                super.onCancelled(gestureDescription);
                                Log.d("Scroll", "----模拟手势失败-----");
                            }
                        }, null);
                    }
                }).start();
            }
        });
        mParams.gravity = Gravity.END | Gravity.BOTTOM;
        mWindowManager.addView(mScrollButtonView,mParams);
        return mScrollButtonView;
    }

}
