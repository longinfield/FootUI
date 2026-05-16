package cn.edu.tsinghua.footinputdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cn.edu.tsinghua.footinputdemo.views.SideBarArrow;

/**
 * a receiver to accept broadcast form launcher to hide the sidebar.
 *
 * @author majh
 */
public class SideBarHideReceiver extends BroadcastReceiver {

    private SideBarArrow mSideArrow = null;

    private static final String ACTION_HIDE = "com.android.sidebar.ACTION_HIDE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_HIDE)) {
            if (null != mSideArrow) {
                mSideArrow.launcherInvisibleSideBar();
            }
        }
    }

    public void setSideBar(SideBarArrow sideArrow) {
        mSideArrow = sideArrow;
    }

}
