package cn.edu.tsinghua.footinputdemo.views;

import android.annotation.SuppressLint;
import android.content.Context;

import android.support.v7.widget.AppCompatImageView;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.util.Log;
import cn.edu.tsinghua.footinputdemo.R;
import cn.edu.tsinghua.footinputdemo.SideBarService;

public class Cursor{
    private Context mContext;
    private LinearLayout mCursorView;
    private WindowManager mWindowManager;
    private LinearLayout mArrowView;
    private SideBarService mSideBarService;
    private WindowManager.LayoutParams mParams;
    public AppCompatImageView cursor;


    private int posx;
    private int posy;
    public static final String TAG = "--MyService--";


    @SuppressLint("ClickableViewAccessibility")
    public LinearLayout getView(Context context,
                                SideBarService sideBarService,
                                WindowManager windowManager,
                                WindowManager.LayoutParams Params) {
        mContext = context;
        mWindowManager = windowManager;
        mParams=Params;
        mSideBarService=sideBarService;

        // get layout
        LayoutInflater inflater = LayoutInflater.from(context);
        mCursorView= (LinearLayout) inflater.inflate(R.layout.layout_cursor, null);

        cursor=mCursorView.findViewById(R.id.cursor);

        cursor.setLongClickable(true);
        cursor.setOnTouchListener(new View.OnTouchListener() {
            private WindowManager.LayoutParams updateParams=mParams;
            int x,y;
            float TouchedX,TouchedY;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        x=updateParams.x;
                        y=updateParams.y;

                        TouchedX=motionEvent.getRawX();
                        TouchedY=motionEvent.getRawY();

                        break;

                    case MotionEvent.ACTION_MOVE:
                        updateParams.x=(int) (x+(motionEvent.getRawX()-TouchedX));
                        updateParams.y=(int) (y+(motionEvent.getRawY()-TouchedY));

                        mWindowManager.updateViewLayout(mCursorView,updateParams);
                        Log.d("updateParams.x",String.valueOf(updateParams.x));
                        Log.d("updateParams.y",String.valueOf(updateParams.y));

                        break;
                    case MotionEvent.ACTION_UP:
                        view.performClick();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });

        mWindowManager.addView(mCursorView,mParams);
        return mCursorView;
    }

    public void updateView(double x, double y){
        WindowManager.LayoutParams updateParams=mParams;
        //int x,y;
        //float TouchedX,TouchedY;
        updateParams.x=(int)x;
        updateParams.y=(int)y;
        mWindowManager.updateViewLayout(mCursorView,updateParams);

    }



}
