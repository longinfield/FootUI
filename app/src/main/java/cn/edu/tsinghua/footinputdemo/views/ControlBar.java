package cn.edu.tsinghua.footinputdemo.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.support.v7.widget.AppCompatImageView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import cn.edu.tsinghua.footinputdemo.R;
import cn.edu.tsinghua.footinputdemo.utils.DisplayUtil;
import cn.edu.tsinghua.footinputdemo.utils.SystemBrightness;
import cn.edu.tsinghua.footinputdemo.utils.SystemVolume;

/**
 * A bar to control brightness and volume
 *
 * @author majh
 */
public class ControlBar {

    public WindowManager.LayoutParams mParams;
    public HorizontalSeekBar seekBar;

    @SuppressLint("RtlHardcoded")
    public LinearLayout getView(final Context context,int tag, final SideBarContent sideBarContent) {
        mParams = new WindowManager.LayoutParams();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }else {
            mParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        mParams.format = PixelFormat.RGBA_8888;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.gravity = Gravity.LEFT | Gravity.TOP;
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        LinearLayout seekBarLayout = (LinearLayout) inflater.inflate(R.layout.layout_horizontal_seekbar, null);
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        seekBarLayout.measure(w,h);
        // left or right seekbar
        mParams.x = DisplayUtil.dp2px(context,200) - seekBarLayout.getMeasuredWidth();
        mParams.y = DisplayUtil.getScreenHeight(context)/2+500;
        mParams.windowAnimations = R.style.LeftSeekBarAnim;

        //HorizontalSeekBar seekBar = seekBarLayout.findViewById(R.id.sb_);
        seekBar = seekBarLayout.findViewById(R.id.sb_);
        AppCompatImageView plus = seekBarLayout.findViewById(R.id.plus);
        AppCompatImageView less = seekBarLayout.findViewById(R.id.less);
        if(tag == 0) {
            // brightness control
            plus.setImageDrawable(context.getDrawable(R.drawable.ic_brightness_low_black_24dp));
            less.setImageDrawable(context.getDrawable(R.drawable.ic_brightness_high_black_24dp));
            // brightness range 0~255
            seekBar.setMax(255);
            seekBar.setProgress(SystemBrightness.getBrightness(context));
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    SystemBrightness.setBrightness(context, progress);
                    sideBarContent.removeOrSendMsg(true,true);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }else if(tag == 1) {
            // volume control
            plus.setImageDrawable(context.getDrawable(R.drawable.ic_volume_down_black_24dp));
            less.setImageDrawable(context.getDrawable(R.drawable.ic_volume_up_black_24dp));
            // volume range 0~15
            seekBar.setMax(15);
            seekBar.setProgress(SystemVolume.getVolume(context));
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    SystemVolume.setVolume(context, progress);
                    sideBarContent.removeOrSendMsg(true,true);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }
        return seekBarLayout;
    }
}
